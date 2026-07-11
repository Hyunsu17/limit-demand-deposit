package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplicationRepository;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
 class AccountOpenCompensationServiceTest {

    @InjectMocks
    AccountOpenCompensationService accountOpenCompensationService;

    @Mock
    AccountOpenApplicationRepository accountOpenApplicationRepository;

    private static final Long APPLICATION_ID = 1L;

    @Test
    @DisplayName("신청건을_찾을수_없으면_예외가_발생한다")
    void markSystemError_applicationNotFound_Exception_throws() {
        // given
        when(accountOpenApplicationRepository.findById(any())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> accountOpenCompensationService.markSystemError(APPLICATION_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }
}
