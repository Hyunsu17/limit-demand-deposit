package com.hyunsu.limitdeposit.transaction.domain;

import com.hyunsu.limitdeposit.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TRANS_RAW — 거래원본. 성공/실패와 무관하게 수신 전문을 가장 먼저 선적재한다(부인방지·장애 재처리 근거).
 * raw_data(전문 원본)는 불변이며, 처리 결과에 따라 process_status/process_dttm 만 전이된다.
 */
@Entity
@Table(name = "transaction_raw")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionRaw extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "raw_seq")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Column(name = "raw_dttm", nullable = false)
    private LocalDateTime rawDttm;

    @Column(name = "raw_data", nullable = false, columnDefinition = "TEXT")
    private String rawData;

    @Column(name = "acct_no", nullable = false, length = 20)
    private String acctNo;

    @Column(name = "txn_amt", nullable = false, precision = 19, scale = 4)
    private BigDecimal txnAmt;

    @Enumerated(EnumType.STRING)
    @Column(name = "process_status", nullable = false, length = 20)
    private ProcessStatus processStatus;

    @Column(name = "process_dttm")
    private LocalDateTime processDttm;

    private TransactionRaw(ChannelType channelType, String acctNo, BigDecimal txnAmt, String rawData) {
        this.channelType = channelType;
        this.acctNo = acctNo;
        this.txnAmt = txnAmt;
        this.rawData = rawData;
        this.rawDttm = LocalDateTime.now();
        this.processStatus = ProcessStatus.PENDING;
    }

    /**
     * 전문 수신 시점의 선적재 — PENDING 상태로 생성. 검증/원장반영 이전에 호출된다.
     */
    public static TransactionRaw receive(ChannelType channelType, String acctNo, BigDecimal txnAmt, String rawData) {
        return new TransactionRaw(channelType, acctNo, txnAmt, rawData);
    }

    // [Claude] 원장 반영 성공 후 처리완료 전이
    public void markCompleted() {
        this.processStatus = ProcessStatus.COMPLETED;
        this.processDttm = LocalDateTime.now();
    }

    // [Claude] 검증/원장반영 실패 시 처리실패 전이 (전문은 그대로 보존)
    public void markFailed() {
        this.processStatus = ProcessStatus.FAILED;
        this.processDttm = LocalDateTime.now();
    }
}
