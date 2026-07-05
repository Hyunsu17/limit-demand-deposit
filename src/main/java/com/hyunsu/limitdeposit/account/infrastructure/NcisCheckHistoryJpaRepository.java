package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.NcisCheckHistory;
import org.springframework.data.jpa.repository.JpaRepository;

interface NcisCheckHistoryJpaRepository extends JpaRepository<NcisCheckHistory, Long> {
}
