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

    // PESSIMISTIC_WRITE → SELECT ... FOR UPDATE. 같은 계좌의 잔액 변경 거래를 이 단일 행 락으로 직렬화한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.acctNo = :acctNo")
    Optional<Account> findByAcctNoForUpdate(@Param("acctNo") String acctNo);
}
