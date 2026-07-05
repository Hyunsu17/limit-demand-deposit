package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.domain.AccountOpenApplicationRepository;
import com.hyunsu.limitdeposit.account.domain.AccountRepository;
import com.hyunsu.limitdeposit.account.domain.ApplicationStatus;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import com.hyunsu.limitdeposit.customer.domain.Customer;
import com.hyunsu.limitdeposit.customer.domain.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountOpenValidationServiceTest {

    private static final Long CUSTOMER_ID = 1L;

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountOpenApplicationRepository accountOpenApplicationRepository;

    @InjectMocks
    private AccountOpenValidationService validationService;

    private Customer customerAged(int age) {
        return Customer.builder()
                .loginId("test")
                .password("pw")
                .name("테스트")
                .birthDate(LocalDate.now().minusYears(age))
                .build();
    }

    @Test
    @DisplayName("모든_검증을_통과하면_Customer를_반환한다")
    void validate_success() {
        // given
        Customer customer = customerAged(20);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(accountRepository.existsByCustomerId(CUSTOMER_ID)).thenReturn(false);
        when(accountOpenApplicationRepository.existsByCustomerIdAndAppStatus(CUSTOMER_ID, ApplicationStatus.PENDING))
                .thenReturn(false);

        // when
        Customer result = validationService.validate(CUSTOMER_ID);

        // then
        assertThat(result).isEqualTo(customer);
    }

    @Test
    @DisplayName("고객이_존재하지_않으면_CUSTOMER_NOT_FOUND_예외가_발생하고_이후_검증은_호출되지_않는다")
    void validate_customerNotFound_exception_throws() {
        // given
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> validationService.validate(CUSTOMER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CUSTOMER_NOT_FOUND));

        // 고객 조회에서 이미 막혔으니 뒤 단계 mock은 호출조차 되면 안 됨
        verify(accountRepository, never()).existsByCustomerId(any());
        verify(accountOpenApplicationRepository, never()).existsByCustomerIdAndAppStatus(any(), any());
    }

    @Test
    @DisplayName("만_14세_미만이면_UNDER_AGE_예외가_발생하고_1인1계좌/진행중신청_검증은_호출되지_않는다")
    void validate_under_14_age_exception_throws() {
        // given
        Customer customer = customerAged(13);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));

        // when & then
        assertThatThrownBy(() -> validationService.validate(CUSTOMER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNDER_AGE));

        // 나이예외 통과 못했으니 뒤 단계 mock은 호출조차 되면 안 됨
        verify(accountRepository, never()).existsByCustomerId(any());
        verify(accountOpenApplicationRepository, never()).existsByCustomerIdAndAppStatus(any(), any());
    }

    @Test
    @DisplayName("만_14세_이면_통과한다 (MIN_AGE 경계값)")
    void validate_equal_14_age_success() {
        // given
        Customer customer = customerAged(14);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(accountRepository.existsByCustomerId(CUSTOMER_ID)).thenReturn(false);
        when(accountOpenApplicationRepository.existsByCustomerIdAndAppStatus(CUSTOMER_ID, ApplicationStatus.PENDING)).thenReturn(false);

        // when
        Customer result = validationService.validate(CUSTOMER_ID);

        // then
        assertThat(result).isEqualTo(customer);
    }

    @Test
    @DisplayName("이미_계좌가_있으면_DUPLICATE_ACCOUNT_예외가_발생하고_진행중신청_검증은_호출되지_않는다")
    void validate_duplicate_account_exception_throws() {
        // given
        Customer customer = customerAged(20);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(accountRepository.existsByCustomerId(CUSTOMER_ID)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> validationService.validate(CUSTOMER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_ACCOUNT));

        // 중복체크 통과 못했으니 뒤 단계 mock은 호출조차 되면 안 됨
        verify(accountOpenApplicationRepository, never()).existsByCustomerIdAndAppStatus(any(), any());
    }

    @Test
    @DisplayName("진행중인_신청이_있으면_APPLICATION_IN_PROGRESS_예외가_발생한다")
    void validate_application_in_progress_exception_throws() {
        // given
        Customer customer = customerAged(20);
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        when(accountRepository.existsByCustomerId(CUSTOMER_ID)).thenReturn(false);
        when(accountOpenApplicationRepository.existsByCustomerIdAndAppStatus(CUSTOMER_ID, ApplicationStatus.PENDING)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> validationService.validate(CUSTOMER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.APPLICATION_IN_PROGRESS));
    }
}
