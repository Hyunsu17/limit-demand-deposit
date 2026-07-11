package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.account.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * D5 — PostgreSQL SEQUENCE 기반 채번. 시퀀스가 동시 요청에도 중복 없는 값을 보장한다.
 */
@Component
@RequiredArgsConstructor
public class AccountNumberGeneratorImpl implements AccountNumberGenerator {

    private static final String NEXT_ACCT_NO_SQL = "SELECT LPAD(nextval('acct_no_seq')::TEXT, 13, '0')";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String nextAcctNo() {
        return jdbcTemplate.queryForObject(NEXT_ACCT_NO_SQL, String.class);
    }
}
