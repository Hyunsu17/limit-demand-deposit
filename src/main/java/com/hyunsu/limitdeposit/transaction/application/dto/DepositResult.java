package com.hyunsu.limitdeposit.transaction.application.dto;

import com.hyunsu.limitdeposit.transaction.domain.ProcessFailReason;

import java.math.BigDecimal;

/**
 * 입금 처리 결과.
 *
 * <p>[Claude] 거절을 예외가 아닌 반환값으로 표현한다(2026-07-30 Q5). 처리 TX 안에서 예외를 던지면
 * 같은 TX 에서 수행한 TRANS_RAW 의 FAILED + 실패사유 UPDATE 까지 롤백에 휩쓸려, 영구 PENDING 행이
 * 남고 재처리 배치가 그것을 다시 집는다. 거절은 정상 흐름이므로 정상 반환값이 맞다.
 *
 * <p>[Claude] 거절 여부의 정본은 {@code failReason != null} — TRANS_RAW 의 DB CHECK 제약
 * (FAILED ⟺ 사유 NOT NULL)과 같은 규칙을 객체에서도 유지한다. 별도 boolean 플래그를 두면
 * 같은 사실을 두 필드가 인코딩해 서로 어긋날 수 있다.
 */
public record DepositResult(
        Long rawSeq,
        Long txnSeq,
        BigDecimal balanceAfter,
        ProcessFailReason failReason
) {

    /** 원장 반영 확정 — 거래내역이 생성된 경우 */
    public static DepositResult success(Long rawSeq, Long txnSeq, BigDecimal balanceAfter) {
        return new DepositResult(rawSeq, txnSeq, balanceAfter, null);
    }

    /** 거절 — 원장 무변경, TRANS_HISTORY 무기록. 흔적은 TRANS_RAW 의 처리상태 + 사유가 보유한다 */
    public static DepositResult rejected(Long rawSeq, ProcessFailReason failReason) {
        return new DepositResult(rawSeq, null, null, failReason);
    }

    public boolean isRejected() {
        return failReason != null;
    }
}
