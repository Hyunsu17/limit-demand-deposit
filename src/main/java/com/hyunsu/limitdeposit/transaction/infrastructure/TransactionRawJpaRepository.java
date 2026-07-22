package com.hyunsu.limitdeposit.transaction.infrastructure;

import com.hyunsu.limitdeposit.transaction.domain.TransactionRaw;
import org.springframework.data.jpa.repository.JpaRepository;

interface TransactionRawJpaRepository extends JpaRepository<TransactionRaw, Long> {
}
