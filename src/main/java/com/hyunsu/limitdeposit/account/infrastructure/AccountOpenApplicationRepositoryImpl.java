package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplication;
import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplicationRepository;
import com.hyunsu.limitdeposit.account.domain.opening.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AccountOpenApplicationRepositoryImpl implements AccountOpenApplicationRepository {

    private final AccountOpenApplicationJpaRepository jpaRepository;

    @Override
    public AccountOpenApplication save(AccountOpenApplication application) {
        return jpaRepository.save(application);
    }

    @Override
    public Optional<AccountOpenApplication> findById(Long applicationId) {
        return jpaRepository.findById(applicationId);
    }

    @Override
    public boolean existsByCustomerIdAndAppStatus(Long customerId, ApplicationStatus status) {
        return jpaRepository.existsByCustomerIdAndAppStatus(customerId, status);
    }
}
