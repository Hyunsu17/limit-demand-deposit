package com.hyunsu.limitdeposit.account.domain.account.vo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DepositLimitTest {

    private static final BigDecimal BALANCE_LIMIT = new BigDecimal("5000");   // 보관한도
    private static final BigDecimal MONTHLY_LIMIT = new BigDecimal("3000");   // 월입금한도

    private DepositLimit depositLimit;

    @BeforeEach
    void setUp() {
        // given
        depositLimit = DepositLimit.of(BALANCE_LIMIT, MONTHLY_LIMIT);
    }

    @Test
    @DisplayName("보관한도_여유분이_더_작으면_그_값을_입금가능액으로_반환한다")
    void picks_balance_headroom() {
        // given — 보관한도 여유 200 / 월한도 여유 3000
        BigDecimal balance = new BigDecimal("4800");
        BigDecimal accumulated = BigDecimal.ZERO;

        // when
        BigDecimal depositable = depositLimit.depositableAmount(balance, accumulated);

        // then
        assertThat(depositable).isEqualByComparingTo("200");
    }

    @Test
    @DisplayName("월한도_여유분이_더_작으면_그_값을_입금가능액으로_반환한다")
    void picks_monthly_headroom() {
        // given — 보관한도 여유 5000 / 월한도 여유 100
        BigDecimal balance = BigDecimal.ZERO;
        BigDecimal accumulated = new BigDecimal("2900");

        // when
        BigDecimal depositable = depositLimit.depositableAmount(balance, accumulated);

        // then
        assertThat(depositable).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("두_여유분이_같으면_그_값을_반환한다")
    void equal_headroom() {
        // given — 보관한도 여유 3000 / 월한도 여유 3000
        BigDecimal balance = new BigDecimal("2000");
        BigDecimal accumulated = BigDecimal.ZERO;

        // when
        BigDecimal depositable = depositLimit.depositableAmount(balance, accumulated);

        // then
        assertThat(depositable).isEqualByComparingTo("3000");
    }

    @Test
    @DisplayName("여유분이_정확히_0이면_0을_반환한다")
    void headroom_exactly_zero() {
        // given — 잔액이 보관한도와 같아 여유분 0
        BigDecimal balance = BALANCE_LIMIT;
        BigDecimal accumulated = BigDecimal.ZERO;

        // when
        BigDecimal depositable = depositLimit.depositableAmount(balance, accumulated);

        // then
        assertThat(depositable).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("정책_하향으로_여유분이_음수가_되면_0을_반환한다")
    void negative_headroom_is_clamped_to_zero() {
        // given — 잔액 6000 > 보관한도 5000 (여유 -1000)
        BigDecimal balance = new BigDecimal("6000");
        BigDecimal accumulated = BigDecimal.ZERO;

        // when
        BigDecimal depositable = depositLimit.depositableAmount(balance, accumulated);

        // then
        assertThat(depositable).isEqualByComparingTo("0");
        assertThat(depositable.signum()).isNotNegative();
    }

    @Test
    @DisplayName("첫_입금이면_두_한도_중_작은_값이_그대로_입금가능액이_된다")
    void first_deposit() {
        // given — 잔액 0 · 당월 누계 0
        BigDecimal balance = BigDecimal.ZERO;
        BigDecimal accumulated = BigDecimal.ZERO;

        // when
        BigDecimal depositable = depositLimit.depositableAmount(balance, accumulated);

        // then
        assertThat(depositable).isEqualByComparingTo(MONTHLY_LIMIT);
    }

    @Test
    @DisplayName("한도값의_소수부_scale이_달라도_같은_금액으로_판단한다")
    void scale_does_not_affect_amount() {
        // given — DB NUMERIC(19,4) 조회값처럼 scale 이 붙은 한도
        DepositLimit scaled = DepositLimit.of(new BigDecimal("5000.0000"), new BigDecimal("3000"));

        // when
        BigDecimal depositable = scaled.depositableAmount(new BigDecimal("2000"), BigDecimal.ZERO);

        // then
        assertThat(depositable).isEqualByComparingTo("3000");
    }
}
