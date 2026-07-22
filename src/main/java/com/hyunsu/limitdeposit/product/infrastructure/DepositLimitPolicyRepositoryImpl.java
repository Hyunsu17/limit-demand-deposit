package com.hyunsu.limitdeposit.product.infrastructure;

import com.hyunsu.limitdeposit.product.domain.DepositLimitPolicy;
import com.hyunsu.limitdeposit.product.domain.DepositLimitPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DepositLimitPolicyRepositoryImpl implements DepositLimitPolicyRepository {

    private final DepositLimitPolicyJpaRepository jpaRepository;

    @Override
    public Optional<DepositLimitPolicy> findEffective(String depLmtPolicyId, LocalDate baseDate) {
        return jpaRepository.findEffective(depLmtPolicyId, baseDate);
    }
}
