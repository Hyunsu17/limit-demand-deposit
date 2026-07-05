package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.NcisCheckHistory;
import com.hyunsu.limitdeposit.account.domain.NcisCheckHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NcisCheckHistoryRepositoryImpl implements NcisCheckHistoryRepository {

    private final NcisCheckHistoryJpaRepository jpaRepository;

    @Override
    public NcisCheckHistory save(NcisCheckHistory history) {
        return jpaRepository.save(history);
    }

    @Override
    public Optional<NcisCheckHistory> findById(Long ncisCheckId) {
        return jpaRepository.findById(ncisCheckId);
    }
}
