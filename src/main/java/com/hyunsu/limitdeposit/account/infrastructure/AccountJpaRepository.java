package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.account.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface AccountJpaRepository extends JpaRepository<Account, String> {

    boolean existsByCustomerId(Long customerId);

    // PESSIMISTIC_WRITE 로 같은 계좌의 잔액 변경 거래를 단일 행 락으로 직렬화한다.
    // [Claude] 실측(2026-08-19): Hibernate 6 + PostgreSQL 에서 이 쿼리는 FOR UPDATE 가 아니라
    // [Claude] SELECT ... FOR NO KEY UPDATE 로 나간다. 둘끼리는 서로 충돌하므로 직렬화 목적에는 충분하고,
    // [Claude] FOR UPDATE 와의 차이(FK 검사의 KEY SHARE 락까지 막느냐)는 account_ledger 에 FK 가 없어 무의미하다.
    // [Claude] 실제 차단 여부 검증: AccountPessimisticLockTest
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.acctNo = :acctNo")
    Optional<Account> findByAcctNoForUpdate(@Param("acctNo") String acctNo);
}
