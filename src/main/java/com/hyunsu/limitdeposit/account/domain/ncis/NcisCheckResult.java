package com.hyunsu.limitdeposit.account.domain.ncis;

/**
 * NCIS_CHECK_HIST.CHECK_RESULT
 */
public enum NcisCheckResult {
    PROCESSING, // P: 처리중 (선적재 시점)
    APPROVED,   // Y: 개설가능
    REJECTED,   // N: 불가
    ERROR       // E: 오류 (통신오류/타임아웃 — D10)
}
