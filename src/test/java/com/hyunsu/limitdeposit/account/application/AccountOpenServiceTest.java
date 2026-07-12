package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.application.dto.AccountOpenRequest;
import com.hyunsu.limitdeposit.account.domain.Channel;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckResponse;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckResult;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisClient;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountOpenServiceTest {

    private static final Long CUSTOMER_ID = 1L;
    private static final Long APPLICATION_ID = 100L;

    @Mock
    private AccountOpenApplyService applyService;
    @Mock
    private NcisClient ncisClient;
    @Mock
    private AccountOpenConfirmService confirmService;
    @Mock
    private AccountOpenCompensationService compensationService;
    @InjectMocks
    private AccountOpenService accountOpenService;

    private final AccountOpenRequest request = new AccountOpenRequest(CUSTOMER_ID, "PROD001", Channel.NON_FACE_TO_FACE);

    @Test
    @DisplayName("APPLY에_실패하면_CHECK를_실행하지_않는다")
    void openAccount_apply_fail_never_ncis_check() {
        // given
        when(applyService.apply(request)).thenThrow(new BusinessException(ErrorCode.UNDER_AGE));

        // when
        assertThatThrownBy(() -> accountOpenService.openAccount(request))
                .isInstanceOf(BusinessException.class);

        // then
        verify(ncisClient, never()).check(any());
    }


    @Test
    @DisplayName("NCIS_응답이_REJECTED이면_NCIS_CHECK_REJECTED_예외가_발생한다")
    void openAccount_ncisRejected_exception_throws() {
        // given
        when(applyService.apply(request)).thenReturn(APPLICATION_ID);
        when(ncisClient.check(CUSTOMER_ID)).thenReturn(new NcisCheckResponse(NcisCheckResult.REJECTED, "불가"));

        // when & then
        assertThatThrownBy(() -> accountOpenService.openAccount(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NCIS_CHECK_REJECTED));
        verify(confirmService).reject(APPLICATION_ID, "불가");

    }

    @Test
    @DisplayName("NCIS_응답이_PROCESSING이면_규약위반으로_IllegalStateException이_발생한다")
    void openAccount_ncisProcessing_exception_throws() {
        // given
        when(applyService.apply(request)).thenReturn(APPLICATION_ID);
        when(ncisClient.check(CUSTOMER_ID)).thenReturn(new NcisCheckResponse(NcisCheckResult.PROCESSING, null));

        // when & then
        assertThatThrownBy(() -> accountOpenService.openAccount(request))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(confirmService, compensationService);
    }

    @Test
    @DisplayName("NCIS_응답이_ERROR이면_NCIS_COMMUNICATION_ERROR_예외가_발생한다")
    void openAccount_ncisError_exception_throws() {
        // given
        when(applyService.apply(request)).thenReturn(APPLICATION_ID);
        when(ncisClient.check(CUSTOMER_ID)).thenReturn(new NcisCheckResponse(NcisCheckResult.ERROR, "에러"));

        // when & then
        assertThatThrownBy(() -> accountOpenService.openAccount(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NCIS_COMMUNICATION_ERROR));
        verify(confirmService).markCommunicationError(APPLICATION_ID, "에러");
    }

    @Test
    @DisplayName("NCIS_응답이_APPROVED이면_APPROVE에_위임하고_보상은_호출하지_않는다")
    void openAccount_ncisApproved_delegates_approve_never_compensation() {
        // given
        when(applyService.apply(request)).thenReturn(APPLICATION_ID);
        when(ncisClient.check(CUSTOMER_ID)).thenReturn(new NcisCheckResponse(NcisCheckResult.APPROVED, "승인메시지"));

        // when
        accountOpenService.openAccount(request);

        // then
        verify(confirmService).approve(APPLICATION_ID, "승인메시지");
        verifyNoInteractions(compensationService);
    }

    @Test
    @DisplayName("APPROVE가_무결성위반_예외이면_REJECTDUPLICATE_보상_후_DUPLICATE_ACCOUNT_예외가_발생한다")
    void openAccount_approveIntegrityViolation_rejectDuplicate_and_throws() {
        // given
        when(applyService.apply(request)).thenReturn(APPLICATION_ID);
        when(ncisClient.check(CUSTOMER_ID)).thenReturn(new NcisCheckResponse(NcisCheckResult.APPROVED, "승인메시지"));
        doThrow(new DataIntegrityViolationException("UNIQUE 위반")).when(confirmService).approve(APPLICATION_ID, "승인메시지");

        // when & then
        assertThatThrownBy(() -> accountOpenService.openAccount(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_ACCOUNT));
        verify(compensationService).rejectDuplicate(APPLICATION_ID);
    }

    @Test
    @DisplayName("APPROVE가_그_외_예외이면_MARKSYSTEMERROR_보상_후_ACCOUNT_OPEN_FAILED_예외가_발생한다")
    void openAccount_approveUnexpectedException_markSystemError_and_throws() {
        // given
        when(applyService.apply(request)).thenReturn(APPLICATION_ID);
        when(ncisClient.check(CUSTOMER_ID)).thenReturn(new NcisCheckResponse(NcisCheckResult.APPROVED, "승인메시지"));
        doThrow(new CannotAcquireLockException("락획득 실패")).when(confirmService).approve(APPLICATION_ID, "승인메시지");

        // when & then
        assertThatThrownBy(() -> accountOpenService.openAccount(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_OPEN_FAILED));
        verify(compensationService).markSystemError(APPLICATION_ID);

    }

}
