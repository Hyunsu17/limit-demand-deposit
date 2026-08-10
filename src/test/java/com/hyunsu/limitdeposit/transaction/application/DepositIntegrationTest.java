package com.hyunsu.limitdeposit.transaction.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyunsu.limitdeposit.customer.domain.Customer;
import com.hyunsu.limitdeposit.customer.domain.CustomerRepository;
import com.hyunsu.limitdeposit.transaction.domain.ChannelType;
import com.hyunsu.limitdeposit.transaction.presentation.dto.DepositApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * [Claude] 입금 전체 플로우 @SpringBootTest 통합 테스트 — 컨트롤러 → TX1(선적재) → TX2(락·한도·원장) → DB.
 *
 * 계좌개설과 같은 규칙을 따른다: 테스트에 @Transactional 을 붙이지 않는다.
 * TX1/TX2 가 각자 커밋되는 것과, 컨트롤러의 거절 throw 가 그 커밋을 되돌리지 '않는' 것이
 * 검증 대상이라 테스트 트랜잭션으로 감싸면 의미가 사라진다. 정리는 @BeforeEach 가 직접 한다.
 *
 * MockMvc 로 HTTP 계층까지 태우는 이유 — 거절이 4xx 로 나가는 경로(GlobalExceptionHandler)와
 * @Valid 실패가 400 이 되는 경로가 2026-08-03 결정의 일부라 서비스 직접 호출로는 덮이지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepositIntegrationTest {

    /** V4 시드 D01 — 보관한도 5천만, 월한도 3천만 */
    private static final BigDecimal BALANCE_LIMIT = new BigDecimal("50000000");
    private static final BigDecimal MONTHLY_LIMIT = new BigDecimal("30000000");

    private static final String ACCT_NO = "9999999999999";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long customerId;

    @BeforeEach
    void setUp() {
        // [Claude] FK 역순으로 지운다. product / deposit_limit_policy 는 Flyway 시드라 보존
        jdbcTemplate.execute("DELETE FROM transaction_history");
        jdbcTemplate.execute("DELETE FROM transaction_raw");
        jdbcTemplate.execute("DELETE FROM account_ledger_history");
        jdbcTemplate.execute("DELETE FROM account_ledger");
        jdbcTemplate.execute("DELETE FROM ncis_check_history");
        jdbcTemplate.execute("DELETE FROM account_open_application");
        jdbcTemplate.execute("DELETE FROM customer");

        customerId = customerRepository.save(Customer.builder()
                .loginId("deposit-it-user")
                .password("encoded-password")
                .name("입금통합테스트고객")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build()).getId();

        insertActiveAccount(BigDecimal.ZERO);
    }

    /**
     * [Claude] 개설 플로우를 태우지 않고 원장을 직접 넣는다 — 이 테스트의 대상은 입금이고,
     * 개설은 AccountOpenIntegrationTest 가 이미 덮는다.
     */
    private void insertActiveAccount(BigDecimal balance) {
        jdbcTemplate.update(
                "INSERT INTO account_ledger (acct_no, customer_id, prod_cd, dep_lmt_policy_id, pymt_lmt_policy_id, " +
                        "  tax_type, acct_status, balance, available_balance, open_dt) " +
                        "VALUES (?, ?, 'HNDEP001', 'D01', 'P01', 'GENERAL', 'ACTIVE', ?, ?, CURRENT_DATE)",
                ACCT_NO, customerId, balance, balance);
    }

    @Test
    @DisplayName("정상_입금이면_원장이_증가하고_원본_COMPLETED와_거래내역이_남는다")
    void deposit_success_updatesLedger_marksRawCompleted_recordsHistory() throws Exception {
        // given
        Map<String, String> paramMap= Map.of(
                "acctNo", ACCT_NO,
                "amount", "10000",
                "channelType", ChannelType.INTERBANK.name());
        String content =objectMapper.writeValueAsString(paramMap);

        // when & then
        MvcResult result = mockMvc.perform(post("/api/deposits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andExpect(status().isOk())
                .andReturn();
        DepositApiResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), DepositApiResponse.class);

        // then
        // 1) HTTP 200 + 응답 본문(txnSeq, balanceAfter)
        assertThat(response.txnSeq()).isNotNull();
        assertThat(response.balanceAfter()).isEqualByComparingTo("10000");

        // 2) account_ledger.balance 가 입금액만큼 증가 (isEqualByComparingTo)

        Map<String, Object> ledger = jdbcTemplate.queryForMap(
                "SELECT balance FROM account_ledger WHERE acct_no = ?", ACCT_NO);
        assertThat((BigDecimal) ledger.get("balance")).isEqualByComparingTo("10000");

        // 3) transaction_raw 1건 — process_status = COMPLETED, fail_reason IS NULL
        Map<String, Object> transactionRaw = jdbcTemplate.queryForMap(
                "SELECT process_status, fail_reason FROM transaction_raw WHERE acct_no =?", ACCT_NO);
        assertThat(transactionRaw.get("process_status")).isEqualTo("COMPLETED");
        assertThat(transactionRaw.get("fail_reason")).isNull();

        // 4) transaction_history 1건 — balance_after 가 원장 잔액과 일치
        Map<String, Object> transactionHistory = jdbcTemplate.queryForMap(
                "SELECT balance_after FROM transaction_history WHERE acct_no =?", ACCT_NO);
        assertThat((BigDecimal)transactionHistory.get("balance_after")).isEqualByComparingTo((BigDecimal) ledger.get("balance"));

    }

    @Test
    @DisplayName("월입금한도를_초과하면_거절되고_원장은_그대로_원본만_FAILED로_남는다")
    void deposit_exceedsMonthlyLimit_rejected_ledgerUnchanged_rawMarkedFailed() throws Exception {


        // given

        // when

        // then
        // 1) HTTP 4xx + DEPOSIT_LIMIT_EXCEEDED
        // 2) account_ledger.balance 무변경
        // 3) transaction_history 0건 (2026-07-28 — 거절은 무기록)
        // 4) transaction_raw — FAILED + fail_reason 존재
        //    ↑ 컨트롤러의 throw 가 TX1/TX2 커밋을 되돌리지 않는다는 07-30 Q5 설계의 실증
    }
}
