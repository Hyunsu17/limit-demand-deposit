package com.hyunsu.limitdeposit.account.domain;

import java.util.Optional;

public interface AccountOpenApplicationRepository {

    AccountOpenApplication save(AccountOpenApplication application);

    Optional<AccountOpenApplication> findById(Long applicationId);

    /**
     * D7-A — 1차 방어: 진행중(PENDING) 신청 존재 여부 확인
     */
    boolean existsByCustomerIdAndAppStatus(Long customerId, ApplicationStatus status);
}
