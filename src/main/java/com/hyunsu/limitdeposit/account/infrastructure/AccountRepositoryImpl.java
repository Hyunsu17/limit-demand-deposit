package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.account.Account;
import com.hyunsu.limitdeposit.account.domain.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountJpaRepository jpaRepository;

    @Override
    public Account save(Account account) {
        // [Claude] PK(acctNo)가 할당 방식이라 save()가 신규 여부를 SELECT로 확인(merge)한다.
        // [Claude] 개설 1건당 SELECT 1회 추가 — 무시 가능한 수준이라 Persistable 최적화는 보류
        return jpaRepository.save(account);
    }

    @Override
    public boolean existsByCustomerId(Long customerId) {
        return jpaRepository.existsByCustomerId(customerId);
    }
}
