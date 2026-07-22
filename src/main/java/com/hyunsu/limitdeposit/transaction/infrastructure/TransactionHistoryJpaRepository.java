package com.hyunsu.limitdeposit.transaction.infrastructure;

import com.hyunsu.limitdeposit.transaction.domain.TransactionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

interface TransactionHistoryJpaRepository extends JpaRepository<TransactionHistory, Long> {
}
