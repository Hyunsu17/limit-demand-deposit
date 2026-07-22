package com.hyunsu.limitdeposit.transaction.domain;

public interface TransactionHistoryRepository {

    TransactionHistory save(TransactionHistory transactionHistory);

    // [Claude] 월입금누계 실시간 SUM 조회는 Service 설계 시 추가 — dc_type 해석(코드 join vs 상수 필터) 결정 후
}
