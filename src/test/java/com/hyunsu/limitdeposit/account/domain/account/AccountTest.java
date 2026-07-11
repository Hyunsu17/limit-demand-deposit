package com.hyunsu.limitdeposit.account.domain.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {

    private Account account;
    private static final String ACCT_NO = "1000000000001";
    private static final Long CUSTOMER_ID = 1L;
    private static final String PROD_CD = "HNDEP001";
    private static final Long NCIS_CHECK_ID = 1L;
    private static final String DEP_LMT_POLICY_ID = "D01";
    private static final String PYMT_LMT_POLICY_ID = "P01";


    @BeforeEach
    void setUp() {
        // when
        account = Account.open(ACCT_NO, CUSTOMER_ID, PROD_CD, NCIS_CHECK_ID, DEP_LMT_POLICY_ID, PYMT_LMT_POLICY_ID);
    }

    @Test
    @DisplayName("openRecord하면_계좌의_개설시점_상태가_스냅샷으로_기록되고_기본값이_설정된다")
    void open_success() {
        // then
        // path-through test
        assertThat(account).isNotNull();
        assertThat(account.getAcctNo()).isEqualTo(ACCT_NO);
        assertThat(account.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(account.getProdCd()).isEqualTo(PROD_CD);
        assertThat(account.getNcisCheckId()).isEqualTo(NCIS_CHECK_ID);
        assertThat(account.getDepLmtPolicyId()).isEqualTo(DEP_LMT_POLICY_ID);
        assertThat(account.getPymtLmtPolicyId()).isEqualTo(PYMT_LMT_POLICY_ID);

        // default setting test
        assertThat(account.getTaxType()).isEqualTo(TaxType.GENERAL);
        assertThat(account.getAcctStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getAvailableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getOpenDt()).isNotNull();
    }
}
