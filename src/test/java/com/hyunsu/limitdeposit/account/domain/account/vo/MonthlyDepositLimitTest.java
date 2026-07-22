package com.hyunsu.limitdeposit.account.domain.account.vo;

import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MonthlyDepositLimitTest {

    private static final BigDecimal LIMIT = new BigDecimal("1000");
    private MonthlyDepositLimit monthlyDepositLimit;

    @BeforeEach
    void setUp() {
        // given
        monthlyDepositLimit = MonthlyDepositLimit.of(LIMIT);
    }

    @Test
    @DisplayName("누계와_입금액의_합이_한도보다_작으면_통과한다")
    void within_limit() {
        // when & then
        assertThatCode(() -> monthlyDepositLimit.requireNotExceeded(new BigDecimal("300"), new BigDecimal("500")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("누계와_입금액의_합이_한도와_정확히_같으면_통과한다")
    void exactly_at_limit() {
        // when & then
        assertThatCode(() -> monthlyDepositLimit.requireNotExceeded(new BigDecimal("300"), new BigDecimal("700")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("누계와_입금액의_합이_한도를_1원_초과하면_MONTHLY_DEPOSIT_LIMIT_EXCEEDED가_발생한다")
    void over_limit_by_one() {
        // when & then
        assertThatThrownBy(() -> monthlyDepositLimit.requireNotExceeded(new BigDecimal("300"), new BigDecimal("701")))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MONTHLY_DEPOSIT_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("누계가_0인_첫_입금이_한도_이하면_통과한다")
    void first_deposit_within_limit() {
        // when & then
        assertThatCode(() -> monthlyDepositLimit.requireNotExceeded(BigDecimal.ZERO, new BigDecimal("700")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("누계가_0인_첫_입금이_한도를_초과하면_예외가_발생한다")
    void first_deposit_over_limit() {
        // when & then
        assertThatThrownBy(() -> monthlyDepositLimit.requireNotExceeded(BigDecimal.ZERO, new BigDecimal("1100")))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MONTHLY_DEPOSIT_LIMIT_EXCEEDED));
    }
}
