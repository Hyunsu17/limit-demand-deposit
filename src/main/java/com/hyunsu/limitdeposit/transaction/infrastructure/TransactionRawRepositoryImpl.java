package com.hyunsu.limitdeposit.transaction.infrastructure;

import com.hyunsu.limitdeposit.transaction.domain.TransactionRaw;
import com.hyunsu.limitdeposit.transaction.domain.TransactionRawRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TransactionRawRepositoryImpl implements TransactionRawRepository {

    private final TransactionRawJpaRepository jpaRepository;

    @Override
    public TransactionRaw save(TransactionRaw transactionRaw) {
        return jpaRepository.save(transactionRaw);
    }
}
