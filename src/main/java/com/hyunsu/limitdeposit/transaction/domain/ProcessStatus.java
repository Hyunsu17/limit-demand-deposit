package com.hyunsu.limitdeposit.transaction.domain;

/**
 * TRANS_RAW 처리 상태. 수신 시 PENDING → 원장 반영 성공 COMPLETED / 실패 FAILED.
 */
public enum ProcessStatus {
    PENDING,    // 1: 처리대기
    COMPLETED,  // 2: 처리완료
    FAILED      // 3: 처리실패
}
