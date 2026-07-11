package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplicationRepository;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

 class AccountOpenCompensationServiceTest {

    @InjectMocks
    AccountOpenCompensationService accountOpenCompensationService;

    @Mock
    AccountOpenApplicationRepository accountOpenApplicationRepository;

    private final Long APPLICATION_ID = 1L;


    @Test
    @DisplayName("신청건을 찾을 수 없으면 예외")
    void openAccount_apply_fail_never_ncis_check() {
        // given
        when(accountOpenApplicationRepository.findById(any())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountOpenCompensationService.markSystemError(APPLICATION_ID))
                .isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST))


    }
}
