package com.hyunsu.limitdeposit.account.domain.account;

import java.util.Optional;

/**
 * ACCT_LEDGER 포트.
 */
public interface AccountRepository {

    Account save(Account account);

    /**
     * 잔액 변경 거래(입금·지급·해지·이자지급)용 원장 조회 — 행 단위 비관적 락을 획득한다.
     *
     * <p>락 획득이라는 사실을 포트 이름에 드러낸다. 락 순서 규칙("ACCT_LEDGER 를 항상 최우선으로 잠근다")이
     * 순환 대기를 막는 근거이므로, 호출자가 잠금 여부를 알지 못한 채 쓰면 그 규칙을 지킬 수 없다.
     * 잠그지 않는 단순 조회가 필요해지면 별도 메서드로 나눈다.
     */
    Optional<Account> findByAcctNoForUpdate(String acctNo);

    /**
     * D2 — 1인1계좌, ACCT_STATUS 불문 전체 차단
     */
    boolean existsByCustomerId(Long customerId);
}
