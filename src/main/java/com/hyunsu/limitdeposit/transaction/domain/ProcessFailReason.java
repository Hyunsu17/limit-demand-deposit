package com.hyunsu.limitdeposit.transaction.domain;

/**
 * TRANS_RAW 처리실패 사유. ProcessStatus.FAILED 인 행만 값을 가진다.
 *
 * <p>"정상"에 해당하는 값을 두지 않는다 — 정상·미처리는 ProcessStatus 가 이미 표현하므로,
 * 여기에 정상 코드를 두면 같은 사실을 두 컬럼이 인코딩해 서로 어긋날 수 있다.
 *
 * <p>입금불능을 반송이 아닌 거절로 종결(2026-07-28 결정)하면서, 거절의 유일한 추적 근거가
 * 이 사유와 처리상태가 되었다. TRANS_RAW 는 입금·지급 공용 선적재 테이블이므로(2026-07-20 Q5)
 * 사유도 입금 전용으로 좁히지 않는다.
 */
public enum ProcessFailReason {

    ACCOUNT_NOT_FOUND,        // 전문의 계좌번호로 원장을 찾지 못함
    ACCOUNT_NOT_ACTIVE,       // 해지·동결 계좌 (입금프로세스 CASE 1)
    DEPOSIT_LIMIT_EXCEEDED,   // 입금액 > 입금가능액 (입금프로세스 CASE 3)
    SYSTEM_ERROR              // 그 외 처리 중 오류 — 재처리 배치 대상
}
