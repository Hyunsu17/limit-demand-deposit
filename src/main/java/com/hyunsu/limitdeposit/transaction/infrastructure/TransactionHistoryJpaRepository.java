package com.hyunsu.limitdeposit.transaction.infrastructure;

import com.hyunsu.limitdeposit.transaction.domain.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

interface TransactionHistoryJpaRepository extends JpaRepository<TransactionHistory, Long> {

    // (acctNo, txnDt) 조건이 idx_transaction_history_acct_dt 를 그대로 탄다
    // COALESCE — 거래가 없으면 SUM 이 NULL 이라 당월 최초 입금에서 NPE 가 된다
    @Query("""
            SELECT COALESCE(SUM(h.txnAmt), 0)
            FROM TransactionHistory h
            WHERE h.acctNo = :acctNo
              AND h.txnDt BETWEEN :from AND :to
              AND h.txnCode = :txnCode
            """)
    BigDecimal sumAmountByTxnCode(@Param("acctNo") String acctNo,
                                  @Param("from") LocalDate from,
                                  @Param("to") LocalDate to,
                                  @Param("txnCode") String txnCode);
}
