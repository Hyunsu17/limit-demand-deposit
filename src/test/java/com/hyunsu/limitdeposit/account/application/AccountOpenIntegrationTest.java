package com.hyunsu.limitdeposit.account.application;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.hyunsu.limitdeposit.account.application.dto.AccountOpenRequest;
import com.hyunsu.limitdeposit.account.domain.Channel;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckResponse;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckResult;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisClient;
import com.hyunsu.limitdeposit.common.exception.BusinessException;
import com.hyunsu.limitdeposit.common.exception.ErrorCode;
import com.hyunsu.limitdeposit.customer.domain.Customer;
import com.hyunsu.limitdeposit.customer.domain.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [Claude] 계좌개설 전체 플로우 @SpringBootTest 통합 테스트.
 * 실제 구성으로 검증한다: Testcontainers PostgreSQL(Flyway V1~V3) + WireMock(NCIS 대체) + 실제 빈 전부.
 *
 * 의도적으로 테스트에 @Transactional을 붙이지 않는다 — TX1/TX2/보상(REQUIRES_NEW)의
 * "실제 커밋 경계"가 검증 대상이라, 테스트 트랜잭션으로 감싸면 전부 한 TX로 합쳐져 의미가 사라진다.
 * 커밋이 실제로 일어나므로 롤백 대신 @BeforeEach에서 데이터를 직접 정리한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class AccountOpenIntegrationTest {

    // [Claude] static 초기화 블록에서 기동 — @DynamicPropertySource가 평가되는 컨텍스트 생성
    // [Claude] 시점보다 항상 먼저 떠 있음을 보장한다 (@BeforeAll은 컨텍스트 생성보다 늦을 수 있음)
    private static final WireMockServer NCIS_SERVER = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        NCIS_SERVER.start();
    }

    @DynamicPropertySource
    static void overrideNcisBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("ncis.base-url", NCIS_SERVER::baseUrl);
    }

    @Autowired
    private AccountOpenService accountOpenService;
    @Autowired
    private AccountOpenApplyService applyService;
    @Autowired
    private AccountOpenConfirmService confirmService;
    @Autowired
    private NcisClient ncisClient;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long customerId;

    @BeforeEach
    void setUp() {
        NCIS_SERVER.resetAll();
        // [Claude] product는 Flyway 시드(HNDEP001)라 지우지 않는다 — 지우면 이후 상품 조회가 깨진다
        jdbcTemplate.execute("DELETE FROM account_ledger_history");
        jdbcTemplate.execute("DELETE FROM account_ledger");
        jdbcTemplate.execute("DELETE FROM ncis_check_history");
        jdbcTemplate.execute("DELETE FROM account_open_application");
        jdbcTemplate.execute("DELETE FROM customer");

        customerId = customerRepository.save(Customer.builder()
                .loginId("it-user")
                .password("encoded-password")
                .name("통합테스트고객")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build()).getId();
    }

    // [Claude] 배관 검증(smoke) — WireMock 스텁 ↔ NcisApiClient 전문 왕복이 동작하는지만 확인.
    // [Claude] 시나리오 1~5(정상 개설/반려/통신오류/롤백·보상/D7-B 예외타입)는 이 아래에 함께 작성한다.
    @Test
    @DisplayName("WireMock_스텁으로_NCIS_전문_왕복이_동작한다")
    void ncisClient_roundTrip_with_wiremock() {
        stubNcis("Y", "가입내역 없음");

        NcisCheckResponse response = ncisClient.check(customerId);

        assertThat(response.result()).isEqualTo(NcisCheckResult.APPROVED);
        assertThat(response.message()).isEqualTo("가입내역 없음");
        NCIS_SERVER.verify(postRequestedFor(urlEqualTo("/api/ncis/duplicate-check"))
                .withRequestBody(matchingJsonPath("$.customerId", equalTo(String.valueOf(customerId)))));
    }

    // [Claude] NCIS 응답 스텁 헬퍼 — result: Y(승인) / N(반려) / E(오류)
    private void stubNcis(String result, String message) {
        NCIS_SERVER.stubFor(post(urlEqualTo("/api/ncis/duplicate-check"))
                .willReturn(okJson("{\"result\":\"%s\",\"message\":\"%s\"}".formatted(result, message))));
    }

    @Test
    @DisplayName("정상_개설이면_계좌원장이_생성된다")
    void openAccount_approved_creates_account_ledger() {
        // given
        stubNcis("Y", "가입내역 없음");
        AccountOpenRequest request = new AccountOpenRequest(customerId, "HNDEP001", Channel.NON_FACE_TO_FACE);

        // when
        accountOpenService.openAccount(request);

        // then — 일단 이것만
        Map<String, Object> application = jdbcTemplate.queryForMap(
                "SELECT * FROM account_open_application WHERE customer_id =?", customerId);
        assertThat(application.get("app_status")).isEqualTo("APPROVED");

        Map<String, Object> ncisCheckHist = jdbcTemplate.queryForMap(
                "SELECT ncs.check_result, ncs.res_msg " +
                        "FROM ncis_check_history ncs JOIN account_open_application apl " +
                        "  ON ncs.application_id = apl.application_id " +
                        "WHERE apl.customer_id = ?", customerId);;
        assertThat(ncisCheckHist.get("check_result")).isEqualTo("APPROVED");

        Map<String, Object> ledger = jdbcTemplate.queryForMap(
                "SELECT * FROM account_ledger WHERE customer_id = ?", customerId);
        assertThat(ledger.get("acct_status")).isEqualTo("ACTIVE");
        assertThat((BigDecimal) ledger.get("balance")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ledger.get("acct_no").toString()).hasSize(13);

        Map<String, Object> ledgerHist = jdbcTemplate.queryForMap(
                "SELECT alh.acct_no, alh.acct_status " +
                        "FROM account_ledger_history alh JOIN account_ledger al " +
                        "ON alh.acct_no = al.acct_no "+
                        "WHERE customer_id = ?", customerId);
        assertThat(ledgerHist.get("acct_status")).isEqualTo("ACTIVE");
        assertThat(ledgerHist.get("acct_no")).isEqualTo(ledger.get("acct_no"));
    }

    @Test
    @DisplayName("NCIS_응답이_N이면_신청이_반려되고_원장은_생성되지_않는다")
    void openAccount_ncisRejected_marksApplicationRejected_and_noLedgerCreated() {
        // given
        stubNcis("N", "불가");
        AccountOpenRequest request = new AccountOpenRequest(customerId, "HNDEP001", Channel.NON_FACE_TO_FACE);

        // when & then
        assertThatThrownBy(() -> accountOpenService.openAccount(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NCIS_CHECK_REJECTED));

        Map<String, Object> application = jdbcTemplate.queryForMap(
                "SELECT * FROM account_open_application WHERE customer_id = ?", customerId);
        assertThat(application.get("app_status")).isEqualTo("REJECTED");

        Map<String, Object> ncisCheckHist = jdbcTemplate.queryForMap(
                "SELECT ncs.check_result, ncs.res_msg " +
                        "FROM ncis_check_history ncs JOIN account_open_application apl " +
                        "  ON ncs.application_id = apl.application_id " +
                        "WHERE apl.customer_id = ?", customerId);
        assertThat(ncisCheckHist.get("check_result")).isEqualTo("REJECTED");
        assertThat(ncisCheckHist.get("res_msg")).isEqualTo("불가");

        Long ledgerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_ledger WHERE customer_id = ?", Long.class, customerId);
        assertThat(ledgerCount).isZero();
    }

    @Test
    @DisplayName("NCIS_통신_실패(5xx)이면_통신오류로_수렴하고_원장은_생성되지_않는다")
    void openAccount_ncisCommunicationFailure_marksApplicationCommError_and_noLedgerCreated() {
        // given — 실제 네트워크 계층 실패를 재현 (지연 대신 500으로 즉시 재현, read-timeout 5s를 기다리지 않도록)
        NCIS_SERVER.stubFor(post(urlEqualTo("/api/ncis/duplicate-check"))
                .willReturn(serverError()));
        AccountOpenRequest request = new AccountOpenRequest(customerId, "HNDEP001", Channel.NON_FACE_TO_FACE);

        // when & then
        assertThatThrownBy(() -> accountOpenService.openAccount(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NCIS_COMMUNICATION_ERROR));

        Map<String, Object> application = jdbcTemplate.queryForMap(
                "SELECT * FROM account_open_application WHERE customer_id = ?", customerId);
        assertThat(application.get("app_status")).isEqualTo("COMM_ERROR");

        Map<String, Object> ncisCheckHist = jdbcTemplate.queryForMap(
                "SELECT ncs.check_result FROM ncis_check_history ncs " +
                        "JOIN account_open_application apl ON ncs.application_id = apl.application_id " +
                        "WHERE apl.customer_id = ?", customerId);
        assertThat(ncisCheckHist.get("check_result")).isEqualTo("ERROR");

        Long ledgerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_ledger WHERE customer_id = ?", Long.class, customerId);
        assertThat(ledgerCount).isZero();
    }

    @Test
    @DisplayName("TX2가_실패하면_원장은_생성되지_않고_보상은_별도_트랜잭션으로_커밋된다")
    void openAccount_tx2Fails_rollsBackAtomically_compensationSurvivesInSeparateTransaction(){

        // given
        stubNcis("Y", "가입내역 없음");
        AccountOpenRequest request = new AccountOpenRequest(customerId, "NOT_EXIST", Channel.NON_FACE_TO_FACE);

        // when & then
        assertThatThrownBy(() -> accountOpenService.openAccount(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_OPEN_FAILED));

        Map<String, Object> application = jdbcTemplate.queryForMap(
                "SELECT * FROM account_open_application WHERE customer_id = ?", customerId);
        assertThat(application.get("app_status")).isEqualTo("SYSTEM_ERROR");

        //
        Map<String, Object> ncisCheckHist = jdbcTemplate.queryForMap(
                "SELECT ncs.check_result FROM ncis_check_history ncs " +
                        "JOIN account_open_application apl ON ncs.application_id = apl.application_id " +
                        "WHERE apl.customer_id = ?", customerId);
        assertThat(ncisCheckHist.get("check_result")).isEqualTo("PROCESSING");

        Long ledgerCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM account_ledger WHERE customer_id = ?", Long.class, customerId);
        assertThat(ledgerCount).isZero();
    }

    @Test
    @DisplayName("customer_id가_이미_사용중이면_approve가_어떤_예외로_실패하는지_확인한다")
    void approve_whenCustomerIdAlreadyTaken_throwsPersistenceExceptionAtCommitFlush() {
        // given
        // 신청서 작성
        AccountOpenRequest request = new AccountOpenRequest(customerId, "HNDEP001", Channel.NON_FACE_TO_FACE);
        Long applicationID = applyService.apply(request);
        // 중간에 원장이 생성되는 케이스 재현
        jdbcTemplate.update(
                "INSERT INTO account_ledger (acct_no, customer_id, prod_cd, dep_lmt_policy_id, pymt_lmt_policy_id, " +
                        "  tax_type, acct_status, balance, available_balance, open_dt) " +
                        "VALUES ('9999999999999', ?, 'HNDEP001', 'D01', 'P01', 'GENERAL', 'ACTIVE', 0, 0, CURRENT_DATE)",
                customerId);

        // when & then
        assertThatThrownBy(() -> confirmService.approve(applicationID, "성공"))
                .isInstanceOf(DataIntegrityViolationException.class);

    }

}
