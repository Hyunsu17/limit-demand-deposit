package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.account.AccountNumberGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D5 — acct_no_seq 시퀀스 채번. 네이티브 SQL(nextval + LPAD)이라 실제 Postgres에서만 검증 가능.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AccountNumberGeneratorImpl.class)
@ActiveProfiles("test")
class AccountNumberGeneratorImplTest {

    @Autowired
    private AccountNumberGenerator accountNumberGenerator;

    @Test
    @DisplayName("채번된_계좌번호는_13자리_숫자_문자열이다")
    void nextAcctNo_returns_13_digit_number() {
        // when
        String acctNo = accountNumberGenerator.nextAcctNo();

        // then
        assertThat(acctNo).matches("\\d{13}");
    }

    @Test
    @DisplayName("연속_채번하면_서로_다른_증가하는_번호가_발급된다")
    void nextAcctNo_successive_calls_are_distinct_and_increasing() {
        // when
        String first = accountNumberGenerator.nextAcctNo();
        String second = accountNumberGenerator.nextAcctNo();

        // then
        assertThat(second).isNotEqualTo(first);
        assertThat(Long.parseLong(second)).isGreaterThan(Long.parseLong(first));
    }
}
