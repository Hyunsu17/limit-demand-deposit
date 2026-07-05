package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.application.dto.AccountOpenRequest;
import com.hyunsu.limitdeposit.account.domain.Channel;
import com.hyunsu.limitdeposit.account.domain.NcisCheckResponse;
import com.hyunsu.limitdeposit.account.domain.NcisCheckResult;
import com.hyunsu.limitdeposit.account.domain.NcisClient;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        inOrder(applyService, ncisClient);
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
    }

    @Test
    @DisplayName("NCIS_응답이_ERROR이면_NCIS_COMMUNICATION_ERROR_예외가_발생한다")
    void openAccount_ncisError_exception_throws() {
        // given
        when(applyService.apply(request)).thenReturn(APPLICATION_ID);
        when(ncisClient.check(CUSTOMER_ID)).thenReturn(new NcisCheckResponse(NcisCheckResult.ERROR, null));

        // when & then
        assertThatThrownBy(() -> accountOpenService.openAccount(request))
                .isInstanceOfSatisfying(BusinessException.class, ex->assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NCIS_COMMUNICATION_ERROR));
    }

    // TODO : Week2에서 구현후 대체
    @Test
    @DisplayName("NCIS_응답이_APPROVED이면_TX2_미구현으로_UnsupportedOperationException이_발생한다 (Week2에서 TX2 구현 후 이 테스트는 대체될 임시 케이스)")
    void openAccount_ncisApproved_exception_throws() {
        // given
        when(applyService.apply(request)).thenReturn(APPLICATION_ID);
        when(ncisClient.check(CUSTOMER_ID)).thenReturn(new NcisCheckResponse(NcisCheckResult.APPROVED, null));

        // when & then
        assertThatThrownBy(() -> accountOpenService.openAccount(request))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining(APPLICATION_ID.toString());
    }

}
