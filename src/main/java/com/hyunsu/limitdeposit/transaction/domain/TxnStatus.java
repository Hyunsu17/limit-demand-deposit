package com.hyunsu.limitdeposit.transaction.domain;

/**
 * TRANS_HISTORY 확정 거래 상태.
 *
 * <p>NORMAL 하나만 둔다. 취소·환불은 원거래를 UPDATE 할지(상태전이) 반대거래 행을 추가할지(append-only)가
 * 미결이고, 그 결정이 필요한 경로 — 착오입금 회수·이자 정정·카톡 환불 — 가 전부 현재 스코프 밖이다.
 * 쓰이지 않을 상태값을 미리 두면 "누가 언제 이 값으로 UPDATE 하는가"에 답이 없는 컬럼이 남는다.
 *
 * <p>FAILED 는 두면 안 되는 값이었다 — 실패 거래는 원장이 움직이지 않아 balance_after 에 넣을 값이 없고,
 * "고객 거래내역은 원장이 실제로 움직인 거래만 담는다"(2026-07-28 결정)와 충돌한다.
 * 거절의 흔적은 TRANS_RAW 의 process_status + fail_reason 이 보유한다.
 */
public enum TxnStatus {
    NORMAL      // 1: 정상
}
