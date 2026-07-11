package com.hyunsu.limitdeposit.account.domain.account;

/**
 * ACCT_LEDGER.TAX_TYPE — D9: 현재 스코프는 GENERAL(일반과세)만 사용, 비과세/세금우대 로직 제외
 */
public enum TaxType {
    GENERAL,      // 1: 일반과세 (15.4%)
    TAX_FREE,     // 2: 비과세
    TAX_PREFERRED // 3: 세금우대
}
