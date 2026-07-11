package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplication;
import com.hyunsu.limitdeposit.account.domain.opening.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

interface AccountOpenApplicationJpaRepository extends JpaRepository<AccountOpenApplication, Long> {

    boolean existsByCustomerIdAndAppStatus(Long customerId, ApplicationStatus appStatus);
}
