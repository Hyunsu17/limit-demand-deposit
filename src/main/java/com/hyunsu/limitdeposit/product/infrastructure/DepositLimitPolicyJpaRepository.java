package com.hyunsu.limitdeposit.product.infrastructure;

import com.hyunsu.limitdeposit.product.domain.DepositLimitPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

interface DepositLimitPolicyJpaRepository extends JpaRepository<DepositLimitPolicy, Long> {

    @Query("""
            SELECT p FROM DepositLimitPolicy p
            WHERE p.depLmtPolicyId = :depLmtPolicyId
              AND p.appySttDt <= :baseDate
              AND p.appyEndDt >= :baseDate
            """)
    Optional<DepositLimitPolicy> findEffective(@Param("depLmtPolicyId") String depLmtPolicyId,
                                               @Param("baseDate") LocalDate baseDate);
}
