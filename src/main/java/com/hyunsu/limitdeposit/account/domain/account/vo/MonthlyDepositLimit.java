package com.hyunsu.limitdeposit.account.domain.account.vo;

import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;

import java.math.BigDecimal;

/**
 * DEP_LMT_POLICY_MST 의 월 입금 한도(값 객체).
 *
 * <p>한도 판단은 Account의 상태(잔액·계좌상태)에 의존하지 않고 오로지
 * "한도 · 당월 누계 · 이번 입금액" 세 값의 관계로만 결정된다. 따라서 Account 엔티티가 아닌
 * 별도 VO가 소유한다. (누계 = 실시간 SUM vs 저장 원장 은 벤치마크 후 결정 — 2026-07-20)
 *
 * <p>데이터(누계·정책)는 Service가 수집하고, 판단은 이 VO가 한다. — 검증 2단계 중 ② 비즈니스
 */
public class MonthlyDepositLimit {

    private final BigDecimal limit;

    private MonthlyDepositLimit(BigDecimal limit) {
        this.limit = limit;
    }

    public static MonthlyDepositLimit of(BigDecimal limit) {
        return new MonthlyDepositLimit(limit);
    }

    /**
     * 이번 입금까지 더한 당월 누계가 한도를 넘지 않음을 보장한다.
     *
     * @param accumulated 이번 입금 직전까지의 당월 입금 누계
     * @param amount      이번 입금액
     */
    public void requireNotExceeded(BigDecimal accumulated, BigDecimal amount) {
        if (accumulated.add(amount).compareTo(limit) > 0) {
            throw new BusinessException(ErrorCode.MONTHLY_DEPOSIT_LIMIT_EXCEEDED);
        }
    }
}
