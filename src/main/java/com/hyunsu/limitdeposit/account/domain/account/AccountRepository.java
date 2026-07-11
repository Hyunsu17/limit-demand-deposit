package com.hyunsu.limitdeposit.account.domain.account;

/**
 * ACCT_LEDGER 포트. 입출금·해지용 조회(findByAcctNo, 비관적 락 등)는 Phase 4+에서 추가된다.
 */
public interface AccountRepository {

    Account save(Account account);

    /**
     * D2 — 1인1계좌, ACCT_STATUS 불문 전체 차단
     */
    boolean existsByCustomerId(Long customerId);
}
