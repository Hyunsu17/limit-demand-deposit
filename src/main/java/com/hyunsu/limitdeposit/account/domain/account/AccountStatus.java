package com.hyunsu.limitdeposit.account.domain.account;

/**
 * ACCT_LEDGER.ACCT_STATUS
 */
public enum AccountStatus {
    ACTIVE, // 1: 정상
    CLOSED, // 2: 해지
    FROZEN  // 3: 동결
}
