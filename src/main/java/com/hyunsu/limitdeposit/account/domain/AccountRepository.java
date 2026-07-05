package com.hyunsu.limitdeposit.account.domain;

/**
 * ACCT_LEDGER 조회 포트. Week2에서 Account(ACCT_LEDGER) 엔티티 확정 시
 * save/findByAccountNo 등 개설·조회 메서드가 추가될 예정.
 */
public interface AccountRepository {

    /**
     * D2 — 1인1계좌, ACCT_STATUS 불문 전체 차단
     */
    boolean existsByCustomerId(Long customerId);
}
