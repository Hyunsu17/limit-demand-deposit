package com.hyunsu.limitdeposit.transaction.infrastructure;

import com.hyunsu.limitdeposit.common.config.JpaConfig;
import com.hyunsu.limitdeposit.transaction.domain.TransactionHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * sumDepositAmount — 월입금누계 실시간 SUM 쿼리를 실제 DB에서 검증한다.
 *
 * 월누계 원장을 두지 않기로 했으므로(2026-07-20 Q1) 이 SUM 하나가 월입금한도 판단의 유일한 원천이다.
 * 틀려도 예외가 나지 않고 한도만 조용히 잘못 걸리므로, 쿼리 자체를 직접 검증한다.
 *
 * TransactionHistory.record()는 txn_dt를 LocalDateTime.now()에서 파생시켜 과거·미래 날짜 거래를
 * 만들 수 없다. 역월 경계값은 JdbcTemplate으로 직접 적재한다 (ProductRepositoryImplTest와 같은 처방).
 *
 * 검증 방식: "잡혀야 할 행"만 심고 합계를 확인하면 필터가 전부 빠져 있어도 통과한다.
 * 그래서 모든 테스트가 "떨어져야 할 행"을 함께 심고, 그것이 합계에 섞이지 않음을 본다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TransactionHistoryRepositoryImpl.class, JpaConfig.class})
@ActiveProfiles("test")
class TransactionHistoryRepositoryImplTest {

    @Autowired
    private TransactionHistoryRepository transactionHistoryRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String ACCT_NO = "1000000000001";
    private static final String OTHER_ACCT_NO = "1000000000002";

    // 고정 날짜를 쓴다 — LocalDate.now() 기준으로 짜면 월말·연말에만 깨지는 flaky 테스트가 된다
    private static final LocalDate MONTH_START = LocalDate.of(2026, 8, 1);
    private static final LocalDate MONTH_END = LocalDate.of(2026, 8, 31);

    private static final String DEPOSIT_CODE = "DEP01";
    // Phase 5에 실제로 들어올 이자입금. 은행이 지급하는 이자는 고객 월입금한도에 세지 않는다(2026-07-30 Q4)
    private static final String INTEREST_CODE = "INT01";

    /**
     * txn_code에 FK가 없어(논리 FK) 시드에 없는 코드도 적재된다.
     * balance_after·txn_status·channel_type은 이 쿼리가 읽지 않지만 NOT NULL이라 채운다 —
     * "테이블이 요구하는 최소 데이터"와 "테스트가 원하는 최소 데이터"는 다르다.
     */
    private void insertHistory(String acctNo, String txnCode, LocalDate txnDt, String amount) {
        jdbcTemplate.update("""
                INSERT INTO transaction_history (acct_no, txn_code, txn_dt, txn_dttm, txn_amt,
                                                 balance_after, txn_status, channel_type)
                VALUES (?, ?, ?, ?, ?, ?, 'NORMAL', 'INTERBANK')
                """, acctNo, txnCode, txnDt, txnDt.atStartOfDay(), new BigDecimal(amount), new BigDecimal(amount));
    }

    private void insertDeposit(LocalDate txnDt, String amount) {
        insertHistory(ACCT_NO, DEPOSIT_CODE, txnDt, amount);
    }

    private BigDecimal sumThisMonth() {
        return transactionHistoryRepository.sumDepositAmount(ACCT_NO, MONTH_START, MONTH_END);
    }

    @Test
    @DisplayName("거래가_한_건도_없으면_null이_아니라_0을_반환한다_COALESCE")
    void sumDepositAmount_noTransaction_returnsZero() {
        // when — 아무것도 적재하지 않는다. SUM의 대상 행이 0건인 상태
        BigDecimal sum = sumThisMonth();

        // then — COALESCE가 없으면 여기서 null이 오고, 호출부 DepositLimit에서 NPE가 된다.
        // 당월 최초 입금은 모든 신규 계좌의 첫 거래이므로 가장 흔한 경로다
        assertThat(sum).isNotNull();
        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("다른_계좌의_입금은_합계에_섞이지_않는다")
    void sumDepositAmount_otherAccount_excluded() {
        // given — 잡혀야 할 행 + 떨어져야 할 행
        insertDeposit(MONTH_START, "500000");
        insertHistory(OTHER_ACCT_NO, DEPOSIT_CODE, MONTH_START, "300000");

        // then — 계좌 필터가 빠지면 800000이 된다
        assertThat(sumThisMonth()).isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    @DisplayName("입금이_아닌_거래코드는_합계에서_제외된다_이자입금은_월한도를_먹지_않는다")
    void sumDepositAmount_nonDepositCode_excluded() {
        // given
        insertDeposit(MONTH_START, "500000");
        insertHistory(ACCT_NO, INTEREST_CODE, MONTH_START, "1200");

        // then — transaction_history에 dc_type이 없어 거래코드 상수로 거르는데(2026-07-22 Q6),
        // 이 필터가 "은행이 준 이자를 고객 월입금한도에 세지 않는다"의 유일한 방어선이다
        assertThat(sumThisMonth()).isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    @DisplayName("역월_경계_시작일과_종료일은_포함하고_그_바깥은_제외한다")
    void sumDepositAmount_monthBoundary_inclusiveBothEnds() {
        // given — 금액을 서로 다른 자릿수로 두면 합계만 보고 어느 행이 샜는지 역산할 수 있다
        insertDeposit(MONTH_START.minusDays(1), "1000");   // 전월 말일 — 제외
        insertDeposit(MONTH_START, "2000");                // 당월 1일 — 포함
        insertDeposit(MONTH_END, "4000");                  // 당월 말일 — 포함
        insertDeposit(MONTH_END.plusDays(1), "8000");      // 익월 1일 — 제외

        // then — 당월=역월(2026-07-30 Q7)이 실제로 구현됐는지의 증명.
        // 말일이 빠지면 매월 말일 입금이 다음 달 한도를 먹는다.
        // 실패 시 7000이면 전월분이, 14000이면 익월분이 샌 것이다
        assertThat(sumThisMonth()).isEqualByComparingTo(new BigDecimal("6000"));
    }

    @Test
    @DisplayName("당월_입금이_여러_건이면_모두_합산된다")
    void sumDepositAmount_multipleDeposits_summed() {
        // given
        insertDeposit(MONTH_START, "500000");
        insertDeposit(MONTH_START.plusDays(10), "300000");
        insertDeposit(MONTH_END, "200000");

        // then — NUMERIC(19,4)라 1000000이 아니라 1000000.0000으로 돌아온다.
        // isEqualTo는 scale까지 비교해 깨지므로 isEqualByComparingTo를 쓴다(2026-07-11 컨벤션)
        assertThat(sumThisMonth()).isEqualByComparingTo(new BigDecimal("1000000"));
    }
}
