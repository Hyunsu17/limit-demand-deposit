package com.hyunsu.limitdeposit.account.domain.ncis;

import java.util.Optional;

public interface NcisCheckHistoryRepository {

    NcisCheckHistory save(NcisCheckHistory history);

    Optional<NcisCheckHistory> findById(Long ncisCheckId);

    /**
     * TX2 — 신청 건에 선적재된 P(처리중) 이력 조회 (신청:이력 = 1:1, TX1에서 함께 생성)
     */
    Optional<NcisCheckHistory> findByApplicationId(Long applicationId);
}
