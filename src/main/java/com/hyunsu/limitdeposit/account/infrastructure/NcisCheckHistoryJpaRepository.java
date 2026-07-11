package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface NcisCheckHistoryJpaRepository extends JpaRepository<NcisCheckHistory, Long> {

    Optional<NcisCheckHistory> findByApplicationId(Long applicationId);
}
