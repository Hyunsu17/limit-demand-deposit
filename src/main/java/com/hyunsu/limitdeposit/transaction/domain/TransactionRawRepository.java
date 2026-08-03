package com.hyunsu.limitdeposit.transaction.domain;

import java.util.Optional;

public interface TransactionRawRepository {

    TransactionRaw save(TransactionRaw transactionRaw);

    /**
     * 선적재된 원본을 처리 트랜잭션에서 다시 읽는다 — 선적재(TX1)와 처리(TX2)가 분리돼 있어
     * TX1 이 반환한 엔티티는 TX2 에서 준영속이다. 여기서 다시 읽어 영속 상태로 만들어야
     * 처리상태 전이(markCompleted/markFailed)가 더티체킹으로 반영된다.
     */
    Optional<TransactionRaw> findById(Long rawSeq);
}
