package com.hyunsu.limitdeposit.transaction.domain;

/**
 * TRANS_HISTORY 확정 거래 상태.
 */
public enum TxnStatus {
    NORMAL,     // 1: 정상
    CANCELLED,  // 2: 취소
    REFUNDED,   // 3: 환불
    FAILED      // 4: 실패
}
