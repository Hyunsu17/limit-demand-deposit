package com.hyunsu.limitdeposit.account.domain;

/**
 * ACCT_OPEN_APPLICATION.APP_STATUS
 */
public enum ApplicationStatus {
    PENDING,      // 1: 신청중
    APPROVED,     // 2: 승인
    REJECTED,     // 3: 반려
    ABANDONED,    // 4: 이탈
    COMM_ERROR,   // 5: 통신오류 (NCIS 타임아웃 포함 — D10)
    SYSTEM_ERROR  // 6: 시스템오류 (TX2 실패 보상 — D8)
}
