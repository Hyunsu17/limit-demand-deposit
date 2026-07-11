package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.account.AccountLedgerHistory;
import com.hyunsu.limitdeposit.account.domain.account.AccountLedgerHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountLedgerHistoryRepositoryImpl implements AccountLedgerHistoryRepository {

    private final AccountLedgerHistoryJpaRepository jpaRepository;

    @Override
    public AccountLedgerHistory save(AccountLedgerHistory history) {
        return jpaRepository.save(history);
    }
}
