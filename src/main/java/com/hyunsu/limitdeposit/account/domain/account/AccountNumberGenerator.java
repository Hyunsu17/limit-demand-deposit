package com.hyunsu.limitdeposit.account.domain.account;

/**
 * D5 — 계좌번호 채번 포트. PostgreSQL SEQUENCE 기반 순수 13자리 숫자 채번을 인프라가 구현한다.
 */
public interface AccountNumberGenerator {

    String nextAcctNo();
}
