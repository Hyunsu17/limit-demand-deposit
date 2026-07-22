package com.hyunsu.limitdeposit.product.domain;

import java.time.LocalDate;
import java.util.Optional;

public interface DepositLimitPolicyRepository {

    /**
     * 기준일에 유효한 입금한도정책 조회 — APPY_STT_DT <= 기준일 <= APPY_END_DT
     */
    Optional<DepositLimitPolicy> findEffective(String depLmtPolicyId, LocalDate baseDate);
}
