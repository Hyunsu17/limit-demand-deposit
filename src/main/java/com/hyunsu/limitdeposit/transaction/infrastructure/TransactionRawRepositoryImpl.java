package com.hyunsu.limitdeposit.transaction.infrastructure;

import com.hyunsu.limitdeposit.transaction.domain.TransactionRaw;
import com.hyunsu.limitdeposit.transaction.domain.TransactionRawRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TransactionRawRepositoryImpl implements TransactionRawRepository {

    private final TransactionRawJpaRepository jpaRepository;

    @Override
    public TransactionRaw save(TransactionRaw transactionRaw) {
        return jpaRepository.save(transactionRaw);
    }

    @Override
    public Optional<TransactionRaw> findById(Long rawSeq) {
        return jpaRepository.findById(rawSeq);
    }
}
