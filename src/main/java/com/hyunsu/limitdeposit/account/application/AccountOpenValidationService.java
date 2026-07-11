package com.hyunsu.limitdeposit.account.application;

import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplicationRepository;
import com.hyunsu.limitdeposit.account.domain.account.AccountRepository;
import com.hyunsu.limitdeposit.account.domain.opening.ApplicationStatus;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import com.hyunsu.limitdeposit.customer.domain.Customer;
import com.hyunsu.limitdeposit.customer.domain.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

/**
 * D1 — TX1 진입 전 로컬 사전검증. NCIS 호출(외부 전문) 낭비를 막기 위해
 * 우리가 알 수 있는 것은 우리가 먼저 막는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountOpenValidationService {

    // [Claude] 가입 가능 최소 연령(만 14세) — 매직넘버 금지 규칙에 따라 상수화
    private static final int MIN_AGE = 14;

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AccountOpenApplicationRepository accountOpenApplicationRepository;

    /**
     * 검증 순서: 1.고객 존재 → 2.나이 요건 → 3.1인1계좌(D2) → 4.진행중 신청(D7-A)
     * 통과 시 이후 TX1(신청 등록)에서 사용할 Customer를 반환한다.
     */
    public Customer validate(Long customerId) {
        Customer customer = validateCustomerExists(customerId);
        validateAge(customer);
        validateNoDuplicateAccount(customerId);
        validateNoPendingApplication(customerId);
        return customer;
    }

    private Customer validateCustomerExists(Long customerId) {
        // 인증을 거쳐 들어오지만, customerId가 실재하는지 도메인 차원에서 재확인
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));
    }

    private void validateAge(Customer customer) {
        // [Claude] 만 나이 = 생년월일부터 오늘까지의 '연' 단위 경과 (생일 안 지났으면 -1 자동 반영)
        int age = Period.between(customer.getBirthDate(), LocalDate.now()).getYears();
        if (age < MIN_AGE) {
            throw new BusinessException(ErrorCode.UNDER_AGE);
        }
    }

    private void validateNoDuplicateAccount(Long customerId) {
        // [Claude] D2 — ACCT_STATUS 불문(정상/해지/동결) 하나라도 있으면 차단
        if (accountRepository.existsByCustomerId(customerId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_ACCOUNT);
        }
    }

    private void validateNoPendingApplication(Long customerId) {
        // [Claude] D7-A(1차 방어) — 진행중(PENDING) 신청이 있으면 NCIS 이중 호출 방지 차원에서 차단.
        // [Claude] 동시 요청의 최후 안전망은 D7-B(ACCT_LEDGER.CUSTOMER_ID UNIQUE, TX2)가 담당.
        if (accountOpenApplicationRepository.existsByCustomerIdAndAppStatus(customerId, ApplicationStatus.PENDING)) {
            throw new BusinessException(ErrorCode.APPLICATION_IN_PROGRESS);
        }
    }
}
