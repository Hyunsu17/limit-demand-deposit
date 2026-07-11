package com.hyunsu.limitdeposit.account.domain.account;

import com.hyunsu.limitdeposit.account.domain.Channel;
import com.hyunsu.limitdeposit.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * ACCT_LEDGER_HIST — 계좌원장 변경 이력.
 * 개설 시 최초 이력을 INSERT하고, 이후 원장 정보 변경 시
 * 기존 이력의 APPY_END_DT를 마감(UPDATE)하고 새 이력을 INSERT하는 패턴으로 관리한다.
 */
@Entity
@Table(name = "account_ledger_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountLedgerHistory extends BaseEntity {

    // [Claude] 최초 개설 이력의 적용종료일 — 다음 변경 전까지 유효를 의미하는 상한값
    private static final LocalDate MAX_APPLY_END_DATE = LocalDate.of(9999, 12, 31);
    private static final String OPEN_CHANGE_REASON = "신규개설";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hist_id")
    private Long id;

    @Column(name = "acct_no", nullable = false, length = 20)
    private String acctNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "chg_type", nullable = false, length = 30)
    private LedgerChangeType chgType;

    @Column(name = "chg_rsn", length = 200)
    private String chgRsn;

    // [Claude] 입력채널은 도메인마다 달라지는 개념이 아니므로 ERD의 CHG_CHNL 코드 대신 공용 Channel enum 재사용
    // [Claude] (ERD의 CHG_CHNL 코드 정의는 실제와 다름 — 위키 쪽 정정 필요)
    @Enumerated(EnumType.STRING)
    @Column(name = "chg_chnl", length = 20)
    private Channel chgChnl;

    @Column(name = "appy_stt_dt", nullable = false)
    private LocalDate appySttDt;

    @Column(name = "appy_end_dt")
    private LocalDate appyEndDt;

    // --- 변경 시점의 원장 상태 스냅샷 ---

    @Column(name = "prod_cd", nullable = false, length = 10)
    private String prodCd;

    @Column(name = "dep_lmt_policy_id", nullable = false, length = 3)
    private String depLmtPolicyId;

    @Column(name = "pymt_lmt_policy_id", nullable = false, length = 3)
    private String pymtLmtPolicyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 20)
    private TaxType taxType;

    @Enumerated(EnumType.STRING)
    @Column(name = "acct_status", nullable = false, length = 20)
    private AccountStatus acctStatus;

    private AccountLedgerHistory(Account account, Channel channel) {
        this.acctNo = account.getAcctNo();
        this.chgType = LedgerChangeType.STATUS_CHANGE;
        this.chgRsn = OPEN_CHANGE_REASON;
        this.chgChnl = channel;
        this.appySttDt = account.getOpenDt();
        this.appyEndDt = MAX_APPLY_END_DATE;
        this.prodCd = account.getProdCd();
        this.depLmtPolicyId = account.getDepLmtPolicyId();
        this.pymtLmtPolicyId = account.getPymtLmtPolicyId();
        this.taxType = account.getTaxType();
        this.acctStatus = account.getAcctStatus();
    }

    /**
     * TX2 — 개설 확정 직후 최초 이력 생성 (CHG_TYPE: 계좌상태변경, APPY_END_DT: 9999-12-31)
     */
    public static AccountLedgerHistory openRecord(Account account, Channel channel) {
        return new AccountLedgerHistory(account, channel);
    }
}
