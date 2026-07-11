package com.hyunsu.limitdeposit.account.domain.account;

public interface AccountLedgerHistoryRepository {

    AccountLedgerHistory save(AccountLedgerHistory history);
}
