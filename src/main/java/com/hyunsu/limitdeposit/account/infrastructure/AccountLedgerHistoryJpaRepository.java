package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.account.AccountLedgerHistory;
import org.springframework.data.jpa.repository.JpaRepository;

interface AccountLedgerHistoryJpaRepository extends JpaRepository<AccountLedgerHistory, Long> {
}
