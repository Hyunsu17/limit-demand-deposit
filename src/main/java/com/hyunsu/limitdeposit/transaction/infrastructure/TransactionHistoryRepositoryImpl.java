package com.hyunsu.limitdeposit.transaction.infrastructure;

import com.hyunsu.limitdeposit.transaction.domain.TransactionCodes;
import com.hyunsu.limitdeposit.transaction.domain.TransactionHistory;
import com.hyunsu.limitdeposit.transaction.domain.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class TransactionHistoryRepositoryImpl implements TransactionHistoryRepository {

    private final TransactionHistoryJpaRepository jpaRepository;

    @Override
    public TransactionHistory save(TransactionHistory transactionHistory) {
        return jpaRepository.save(transactionHistory);
    }

    @Override
    public BigDecimal sumDepositAmount(String acctNo, LocalDate from, LocalDate to) {
        // [Claude] "입금만"이라는 업무 의미는 어댑터가 코드값으로 번역한다 — 포트 시그니처에 코드가 새지 않도록
        return jpaRepository.sumAmountByTxnCode(acctNo, from, to, TransactionCodes.DEPOSIT);
    }
}
