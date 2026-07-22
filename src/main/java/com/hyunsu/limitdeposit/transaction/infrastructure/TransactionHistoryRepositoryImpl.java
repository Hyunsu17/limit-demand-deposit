package com.hyunsu.limitdeposit.transaction.infrastructure;

import com.hyunsu.limitdeposit.transaction.domain.TransactionHistory;
import com.hyunsu.limitdeposit.transaction.domain.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TransactionHistoryRepositoryImpl implements TransactionHistoryRepository {

    private final TransactionHistoryJpaRepository jpaRepository;

    @Override
    public TransactionHistory save(TransactionHistory transactionHistory) {
        return jpaRepository.save(transactionHistory);
    }
}
