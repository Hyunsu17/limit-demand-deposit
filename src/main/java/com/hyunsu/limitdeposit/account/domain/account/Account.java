package com.hyunsu.limitdeposit.account.domain.account;

import com.hyunsu.limitdeposit.account.domain.opening.ApplicationStatus;
import com.hyunsu.limitdeposit.common.BaseEntity;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ACCT_LEDGER — 계좌원장. TX2(개설 확정)에서 생성된다.
 * 입출금(deposit/withdraw)·해지 등 잔액 변경 로직은 Phase 4+ 에서 이 객체에 추가된다.
 */
@Entity
@Table(name = "account_ledger")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends BaseEntity {

    // [Claude] D5 — PK는 시퀀스 채번된 13자리 계좌번호. @GeneratedValue가 아닌 할당 방식
    @Id
    @Column(name = "acct_no", length = 20)
    private String acctNo;

    // [Claude] D7-B — UNIQUE 제약이 1인1계좌의 DB 최후 안전망
    @Column(name = "customer_id", nullable = false, unique = true)
    private Long customerId;

    @Column(name = "prod_cd", nullable = false, length = 10)
    private String prodCd;

    // [Claude] 개설 근거 증빙 — Y(개설가능) 결과의 NCIS_CHECK_HIST 참조
    @Column(name = "ncis_check_id")
    private Long ncisCheckId;

    @Column(name = "dep_lmt_policy_id", nullable = false, length = 3)
    private String depLmtPolicyId;

    // [Claude] D6 정정 — 나이 값은 저장하지 않고 계좌 등급(P01/P02) FK만 유지
    @Column(name = "pymt_lmt_policy_id", nullable = false, length = 3)
    private String pymtLmtPolicyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 20)
    private TaxType taxType;

    @Enumerated(EnumType.STRING)
    @Column(name = "acct_status", nullable = false, length = 20)
    private AccountStatus acctStatus;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableBalance;

    @Column(name = "open_dt", nullable = false)
    private LocalDate openDt;

    // [Claude] 마지막 거래일 — deposit/withdraw 시 갱신. 개설 시점엔 거래가 없어 NULL
    @Column(name = "last_txn_dt")
    private LocalDate lastTxnDt;

    private Account(String acctNo, Long customerId, String prodCd, Long ncisCheckId,
                     String depLmtPolicyId, String pymtLmtPolicyId) {
        this.acctNo = acctNo;
        this.customerId = customerId;
        this.prodCd = prodCd;
        this.ncisCheckId = ncisCheckId;
        this.depLmtPolicyId = depLmtPolicyId;
        this.pymtLmtPolicyId = pymtLmtPolicyId;
        this.taxType = TaxType.GENERAL; // D9 — 일반과세 고정, NTAX_LIMT_AMT는 NULL(미매핑)
        this.acctStatus = AccountStatus.ACTIVE;
        this.balance = BigDecimal.ZERO;
        this.availableBalance = BigDecimal.ZERO;
        this.openDt = LocalDate.now();
    }

    /**
     * TX2 — NCIS 승인(Y) 후 개설 확정 팩토리. 잔액 0, 정상(ACTIVE) 상태로 생성된다.
     */
    public static Account open(String acctNo, Long customerId, String prodCd, Long ncisCheckId,
                               String depLmtPolicyId, String pymtLmtPolicyId) {
        return new Account(acctNo, customerId, prodCd, ncisCheckId, depLmtPolicyId, pymtLmtPolicyId);
    }

    private void requireActive(){
        if (!(this.acctStatus == AccountStatus.ACTIVE)) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    public void withdraw(BigDecimal amount) {
        requireActive();
        this.balance = this.balance.subtract(amount);
        this.availableBalance = this.availableBalance.subtract(amount);
        this.lastTxnDt = LocalDate.now();
    }

    public void deposit(BigDecimal amount) {
        requireActive();
        this.balance = this.balance.add(amount);
        this.availableBalance = this.availableBalance.add(amount);
        this.lastTxnDt = LocalDate.now();
    }
}
