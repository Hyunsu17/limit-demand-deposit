package com.hyunsu.limitdeposit.account.domain;

import java.util.Optional;

public interface NcisCheckHistoryRepository {

    NcisCheckHistory save(NcisCheckHistory history);

    Optional<NcisCheckHistory> findById(Long ncisCheckId);
}
