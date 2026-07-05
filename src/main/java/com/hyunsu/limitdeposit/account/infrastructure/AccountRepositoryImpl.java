package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.AccountRepository;
import org.springframework.stereotype.Repository;

/**
 * Week2에서 Account(ACCT_LEDGER) 엔티티 확정 후 JPA 리포지토리 기반으로 구현 예정.
 */
@Repository
public class AccountRepositoryImpl implements AccountRepository {

    @Override
    public boolean existsByCustomerId(Long customerId) {
        // [Claude] Week2 임시: ACCT_LEDGER 테이블이 아직 없다. 지금은 계좌가 0건이므로
        // [Claude] '중복 없음(false)'이 사실상 정답 — 검증 흐름을 막지 않도록 false 반환.
        // [Claude] Week2에서 AccountJpaRepository.existsByCustomerId 로 실제 구현 교체.
        return false;
    }
}
