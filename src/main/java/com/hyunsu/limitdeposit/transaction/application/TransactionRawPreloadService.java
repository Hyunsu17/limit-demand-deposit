package com.hyunsu.limitdeposit.transaction.application;

import com.hyunsu.limitdeposit.transaction.domain.ChannelType;
import com.hyunsu.limitdeposit.transaction.domain.TransactionRaw;
import com.hyunsu.limitdeposit.transaction.domain.TransactionRawRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * TX1 — 거래 요청 원본 선적재. 검증·원장반영보다 먼저 독립 트랜잭션으로 커밋된다(2026-07-22 Q5).
 *
 * <p>[Claude] 처리와 같은 트랜잭션에 두면 시스템 크래시 시 원본까지 함께 롤백되어
 * "성공/실패와 무관하게 적재한다"(2026-07-20 Q5)는 원칙이 문장으로만 남는다. 원칙을 TX 경계로 강제한다.
 *
 * <p>[Claude] 입금 전용이 아니다 — TRANS_RAW 는 수신 전용이 아닌 "거래 요청 원본" 테이블이고
 * 지급도 같은 선적재를 거친다(2026-07-30 Q3). 그래서 시그니처를 입금 커맨드로 좁히지 않았다.
 */
@Service
@RequiredArgsConstructor
public class TransactionRawPreloadService {

    private final TransactionRawRepository transactionRawRepository;

    /**
     * @return 채번된 raw_seq — 처리 TX 가 이 값으로 원본을 다시 읽어 처리상태를 전이시킨다
     */
    @Transactional
    public Long preload(ChannelType channelType, String acctNo, BigDecimal txnAmt, String rawData) {
        TransactionRaw raw = TransactionRaw.receive(channelType, acctNo, txnAmt, rawData);
        return transactionRawRepository.save(raw).getId();
    }
}
