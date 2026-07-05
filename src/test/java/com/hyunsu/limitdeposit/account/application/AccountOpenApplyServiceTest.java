package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.application.dto.AccountOpenRequest;
import com.hyunsu.limitdeposit.account.domain.AccountOpenApplicationRepository;
import com.hyunsu.limitdeposit.account.domain.Channel;
import com.hyunsu.limitdeposit.account.domain.NcisCheckHistoryRepository;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountOpenApplyServiceTest {

    @Mock
    private AccountOpenValidationService validationService;
    @Mock
    private AccountOpenApplicationRepository accountOpenApplicationRepository;
    @Mock
    private NcisCheckHistoryRepository ncisCheckHistoryRepository;

    @InjectMocks
    private AccountOpenApplyService applyService;

    private static final Long CUSTOMER_ID = 1L;

    private AccountOpenRequest makeRequest() {
        return new AccountOpenRequest(CUSTOMER_ID, "테스트", Channel.NON_FACE_TO_FACE);
    }

    @Test
    @DisplayName("검증을_통과하면_신청과_NCIS이력이_순서대로_저장된다.")
    void apply_validation_pass_saveApp_pass_saveNcis_pass() {
        // given
        AccountOpenRequest request = makeRequest();

        // when
        applyService.apply(request);

        // then
        InOrder order= inOrder(validationService, accountOpenApplicationRepository, ncisCheckHistoryRepository);
        order.verify(validationService).validate(CUSTOMER_ID);
        order.verify(accountOpenApplicationRepository).save(any());
        order.verify(ncisCheckHistoryRepository).save(any());
    }

    @Test
    @DisplayName("검증을_실패하면_어떤_repository도_호출되지_않는다")
    void apply_validation_fail_not_process_anyRepository() {
        // given
        AccountOpenRequest request = makeRequest();
        when(validationService.validate(CUSTOMER_ID)).thenThrow(new BusinessException(ErrorCode.UNDER_AGE));

        // when
        assertThatThrownBy(() -> applyService.apply(request)).isInstanceOf(BusinessException.class);

        // then
        verify(accountOpenApplicationRepository, never()).save(any());
        verify(ncisCheckHistoryRepository, never()).save(any());
    }

    // TODO : 오류 발생 시에 rollback test => mock test로는 검증 불가

}
