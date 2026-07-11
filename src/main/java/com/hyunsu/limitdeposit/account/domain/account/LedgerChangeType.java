package com.hyunsu.limitdeposit.account.domain.account;

/**
 * ACCT_LEDGER_HIST.CHG_TYPE
 */
public enum LedgerChangeType {
    LIMIT_POLICY_CHANGE, // 01: 한도정책변경
    STATUS_CHANGE,       // 02: 계좌상태변경 (신규개설/해지 포함)
    LINK_INFO_CHANGE,    // 03: 연결정보변경
    ETC                  // 04: 기타
}
