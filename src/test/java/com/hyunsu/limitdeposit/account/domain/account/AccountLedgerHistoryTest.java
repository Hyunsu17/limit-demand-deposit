package com.hyunsu.limitdeposit.account.domain.account;

import com.hyunsu.limitdeposit.account.domain.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AccountLedgerHistoryTest {

    private static final String ACCT_NO = "1000000000001";
    private static final Long CUSTOMER_ID = 1L;
    private static final String PROD_CD = "HNDEP001";
    private static final Long NCIS_CHECK_ID = 1L;
    private static final String DEP_LMT_POLICY_ID = "D01";
    private static final String PYMT_LMT_POLICY_ID = "P01";
    private static final Channel CHANNEL = Channel.NON_FACE_TO_FACE;
    private static final LocalDate MAX_APPLY_END_DATE = LocalDate.of(9999, 12, 31);

    private Account account;
    private AccountLedgerHistory history;

    @BeforeEach
    void setUp() {
        // when
        account = Account.open(ACCT_NO, CUSTOMER_ID, PROD_CD, NCIS_CHECK_ID, DEP_LMT_POLICY_ID, PYMT_LMT_POLICY_ID);
        history = AccountLedgerHistory.openRecord(account, CHANNEL);
    }

    @Test
    @DisplayName("openRecord하면_계좌의_개설시점_상태가_스냅샷으로_기록되고_최초이력_기본값이_설정된다")
    void openRecord_success() {
        // then
        // account 스냅샷 pass-through
        assertThat(history).isNotNull();
        assertThat(history.getAcctNo()).isEqualTo(account.getAcctNo());
        assertThat(history.getProdCd()).isEqualTo(account.getProdCd());
        assertThat(history.getDepLmtPolicyId()).isEqualTo(account.getDepLmtPolicyId());
        assertThat(history.getPymtLmtPolicyId()).isEqualTo(account.getPymtLmtPolicyId());
        assertThat(history.getTaxType()).isEqualTo(account.getTaxType());
        assertThat(history.getAcctStatus()).isEqualTo(account.getAcctStatus());
        assertThat(history.getAppySttDt()).isEqualTo(account.getOpenDt());
        assertThat(history.getChgChnl()).isEqualTo(CHANNEL);

        // 최초 개설 이력 기본값
        assertThat(history.getChgType()).isEqualTo(LedgerChangeType.STATUS_CHANGE);
        assertThat(history.getChgRsn()).isEqualTo("신규개설");
        assertThat(history.getAppyEndDt()).isEqualTo(MAX_APPLY_END_DATE);
    }
}
