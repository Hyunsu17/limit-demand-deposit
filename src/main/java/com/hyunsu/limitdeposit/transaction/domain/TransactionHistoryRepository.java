package com.hyunsu.limitdeposit.transaction.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransactionHistoryRepository {

    TransactionHistory save(TransactionHistory transactionHistory);

    /**
     * 기간 내 입금액 합계 — 월입금한도 판단의 "당월 누계" 원천. 별도 누계 원장 없이 실시간 SUM 한다(2026-07-20 Q1).
     *
     * <p>거래코드 상수({@code DEP01})로 입금 거래만 걸러낸다 — TRANS_HISTORY 에 dc_type 이 없어
     * TRANS_CODE 조인이 대안이지만, 핫패스에서 조인을 늘리기 전에 상수 필터로 시작한다(2026-07-22 Q6).
     *
     * @return 거래가 없으면 0 (당월 최초 입금)
     */
    BigDecimal sumDepositAmount(String acctNo, LocalDate from, LocalDate to);
}
