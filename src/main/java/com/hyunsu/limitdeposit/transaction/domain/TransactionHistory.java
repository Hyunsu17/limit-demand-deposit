package com.hyunsu.limitdeposit.transaction.domain;

import com.hyunsu.limitdeposit.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TRANS_HISTORY — 확정 거래내역. 원장 변경이 확정된 거래만 적재하며 수정하지 않는다(오류 정정은 반대거래 추가).
 * 월입금누계는 별도 원장 없이 이 테이블의 실시간 SUM 으로 집계한다(2026-07-20 Q1 결정 B).
 * raw_seq 로 원천(TransactionRaw)을, txn_code 로 거래코드(TransactionCode)를 논리적으로 참조한다.
 */
@Entity
@Table(name = "transaction_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txn_seq")
    private Long id;

    @Column(name = "raw_seq")
    private Long rawSeq;

    @Column(name = "acct_no", nullable = false, length = 20)
    private String acctNo;

    @Column(name = "txn_code", nullable = false, length = 10)
    private String txnCode;

    @Column(name = "txn_dt", nullable = false)
    private LocalDate txnDt;

    @Column(name = "txn_dttm", nullable = false)
    private LocalDateTime txnDttm;

    @Column(name = "txn_amt", nullable = false, precision = 19, scale = 4)
    private BigDecimal txnAmt;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_status", nullable = false, length = 20)
    private TxnStatus txnStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Column(name = "description", length = 200)
    private String description;

    private TransactionHistory(Long rawSeq, String acctNo, String txnCode, BigDecimal txnAmt,
                               BigDecimal balanceAfter, ChannelType channelType, String description) {
        this.rawSeq = rawSeq;
        this.acctNo = acctNo;
        this.txnCode = txnCode;
        this.txnAmt = txnAmt;
        this.balanceAfter = balanceAfter;
        this.channelType = channelType;
        this.description = description;
        this.txnDttm = LocalDateTime.now();
        this.txnDt = this.txnDttm.toLocalDate();
        this.txnStatus = TxnStatus.NORMAL;
    }

    /**
     * 원장 반영이 확정된 거래를 정상(NORMAL)으로 기록한다. balanceAfter 는 반영 후 잔액(호출자가 계산).
     */
    public static TransactionHistory record(Long rawSeq, String acctNo, String txnCode, BigDecimal txnAmt,
                                            BigDecimal balanceAfter, ChannelType channelType, String description) {
        return new TransactionHistory(rawSeq, acctNo, txnCode, txnAmt, balanceAfter, channelType, description);
    }
}
