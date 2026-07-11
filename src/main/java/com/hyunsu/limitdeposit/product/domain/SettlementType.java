package com.hyunsu.limitdeposit.product.domain;

/**
 * PROD_MST.SETTLEMENT_TYPE — 이자 결산 주기
 */
public enum SettlementType {
    FOURTH_FRIDAY, // 1: 매월 넷째 금요일
    MONTH_END      // 2: 매월 말일
}
