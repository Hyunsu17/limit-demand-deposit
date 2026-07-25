package com.hyunsu.limitdeposit.account.domain.account.vo;

import java.math.BigDecimal;

/**
 * DEP_LMT_POLICY_MST 의 입금 2중 한도(값 객체).
 *
 * <p>보관한도(잔액 상한)와 월입금한도(당월 누계 상한)는 한 정책 행에서 함께 오고,
 * 입금가능액이 두 한도의 여유분 중 작은 쪽으로 결정되므로 하나의 VO 가 소유한다.
 * 분리하면 MIN 조합이 Service 로 새어나간다.
 *
 * <p>상태를 갖지 않는 순수 계산기 — 잔액·누계는 Service 가 수집해 인자로 넘긴다.
 * 한도 초과를 예외로 던지지 않고 "입금가능액"으로 답하며, 초과 시 전액 반송(return leg)
 * 분기는 Service 가 판단한다. (2026-07-22 결정)
 */
public class DepositLimit {

    private final BigDecimal balanceLimit;   // bal_lmt_amt — 보관한도
    private final BigDecimal monthlyLimit;   // monthly_dp_lmt_amt — 월입금한도

    private DepositLimit(BigDecimal balanceLimit, BigDecimal monthlyLimit) {
        this.balanceLimit = balanceLimit;
        this.monthlyLimit = monthlyLimit;
    }

    public static DepositLimit of(BigDecimal balanceLimit, BigDecimal monthlyLimit) {
        return new DepositLimit(balanceLimit, monthlyLimit);
    }

    /**
     * 지금 이 계좌에 넣을 수 있는 최대 금액.
     *
     * <p>정책이 하향되어 잔액·누계가 이미 한도를 넘어선 경우 여유분이 음수가 되는데,
     * "넣을 수 있는 금액"은 음수일 수 없으므로 0 으로 자른다.
     *
     * @param balance            현재 잔액
     * @param monthlyAccumulated 이번 입금 직전까지의 당월 입금 누계
     * @return 입금가능액 — MIN(보관한도 − 잔액, 월한도 − 누계), 최소 0
     */
    public BigDecimal depositableAmount(BigDecimal balance, BigDecimal monthlyAccumulated) {
        BigDecimal balanceHeadroom = balanceLimit.subtract(balance);
        BigDecimal monthlyHeadroom = monthlyLimit.subtract(monthlyAccumulated);

        return balanceHeadroom.min(monthlyHeadroom).max(BigDecimal.ZERO);
    }
}
