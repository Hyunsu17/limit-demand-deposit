package com.hyunsu.limitdeposit.transaction.domain;

/**
 * TRANS_CODE(거래코드 마스터) 업무코드 상수.
 *
 * <p>거래코드는 Flyway 시드로 적재된 기준정보이고 엔티티는 아직 두지 않았다(2026-07-22 보류).
 * 월입금누계 SUM 이 "입금 거래만" 골라내야 하는데 TRANS_HISTORY 에 dc_type 이 없어 코드로 필터하므로,
 * 그 코드값이 쿼리에 문자열로 박히지 않도록 여기 모은다.
 */
public final class TransactionCodes {

    /** DEP01 — 입금(CREDIT). 월입금누계 집계 대상 */
    public static final String DEPOSIT = "DEP01";

    private TransactionCodes() {
    }
}
