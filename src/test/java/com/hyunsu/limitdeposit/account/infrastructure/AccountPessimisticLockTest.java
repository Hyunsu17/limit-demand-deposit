package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.account.Account;
import com.hyunsu.limitdeposit.account.domain.account.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [Claude] 비관적 락(PESSIMISTIC_WRITE)이 실제로 다른 트랜잭션을 막는지 검증한다.
 *
 * <p>기존 인프라 테스트의 {@code @DataJpaTest} 4종 세트를 쓸 수 없는 이유가 세 겹이다 —
 * ① 테스트 메서드를 트랜잭션으로 감싸므로 트랜잭션이 하나뿐이고(같은 TX 의 재락은 재진입이라 안 막힌다),
 * ② 트랜잭션은 스레드에 바인딩되므로 한 스레드에서 독립적인 TX 2개를 못 연다,
 * ③ 픽스처가 커밋되지 않아 다른 스레드 눈에 계좌가 아예 안 보인다(막히는 게 아니라 '없음'이 된다).
 *
 * <p>그래서 {@code @Transactional} 없는 {@code @SpringBootTest} 이고, 정리는 {@code @BeforeEach} 가 직접 한다
 * (2026-07-12 통합 테스트 컨벤션과 같은 계열).
 */
@SpringBootTest
@ActiveProfiles("test")
class AccountPessimisticLockTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String ACCT_NO = "8888888888888";

    // [Claude] 다른 테스트 클래스와 겹치지 않는 값. account_ledger.customer_id 는 UNIQUE(1인1계좌) 라
    // [Claude] 커밋되는 이 테스트가 흔한 값(1L)을 쓰면 다른 테스트의 저장을 막는다
    private static final Long CUSTOMER_ID = 8888L;

    /** [Claude] 래치 대기 상한. 정상 흐름에서는 즉시 통과하므로, 여기 걸리면 테스트 설계가 틀린 것이다 */
    private static final long LATCH_TIMEOUT_SEC = 5;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM account_ledger_history");
        jdbcTemplate.execute("DELETE FROM account_ledger");

        // [Claude] 테스트에 @Transactional 이 없으므로 이 save 는 즉시 커밋된다 —
        // [Claude] 커밋돼야만 T1 스레드에서 이 계좌 행이 보이고, 그래야 '락' 을 검증할 수 있다
        accountRepository.save(Account.open(ACCT_NO, CUSTOMER_ID, "HNDEP001", 1L, "D01", "P01"));

        executor = Executors.newSingleThreadExecutor();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();

        // [Claude] 이 테스트는 @Transactional 이 없어 저장이 실제로 커밋된다 —
        // [Claude] 뒤를 치우지 않으면 롤백에 의존하는 @DataJpaTest 클래스들이 이 잔여 행에 오염된다.
        // [Claude] @BeforeEach 정리만으로는 '나 자신'만 보호되고 '다음 클래스'는 보호되지 않는다
        jdbcTemplate.execute("DELETE FROM account_ledger_history");
        jdbcTemplate.execute("DELETE FROM account_ledger");
    }

    @Test
    @DisplayName("T1이_락을_쥔_동안_같은_계좌의_락_조회는_막힌다")
    void lock_blocks_concurrent_transaction_on_same_account() throws Exception {
        CountDownLatch lockAcquired = new CountDownLatch(1);  // T1 이 잠갔다 → T2 출발
        CountDownLatch t2Done = new CountDownLatch(1);        // T2 가 끝났다 → T1 커밋

        // [Claude] T1 — 백그라운드에서 락을 쥐고 t2Done 까지 버틴다.
        // [Claude] submit 은 기다리지 않고 즉시 반환하므로 이 아래 코드가 T2 로서 동시에 흐른다
        Future<?> t1 = executor.submit(() -> transactionTemplate.execute(status -> {
            accountRepository.findByAcctNoForUpdate(ACCT_NO);
            lockAcquired.countDown();
            awaitOrFail(t2Done);
            return null;
            // [Claude] 콜백이 끝나는 순간 커밋되고 락이 풀린다 — 그 전에 T2 가 끝나야 한다
        }));

        // [Claude] T1 이 실제로 잠근 뒤에 출발해야 한다. 이게 없으면 T2 가 먼저 도착해 그냥 통과한다
        assertThat(lockAcquired.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS)).isTrue();

        try {
            assertThatThrownBy(() -> transactionTemplate.execute(status -> {
                // [Claude] SET LOCAL 은 이 트랜잭션에만 적용된다 — 운영 쿼리는 그대로 무기한 대기다.
                // [Claude] 테스트를 위해 운영 동작을 바꾸지 않으려고 힌트가 아닌 세션 설정을 쓴다
                jdbcTemplate.execute("SET LOCAL lock_timeout = '500ms'");
                return accountRepository.findByAcctNoForUpdate(ACCT_NO);
            })).isInstanceOf(PessimisticLockingFailureException.class);
        } finally {
            // [Claude] assert 가 실패해도 T1 을 반드시 풀어준다. 안 그러면 테스트가 멈춘 채로 끝난다
            t2Done.countDown();
        }

        t1.get(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    /**
     * [Claude] {@code await()} 는 checked 예외를 던지는데 TransactionCallback 람다는 그것을 밖으로 못 낸다.
     * 테스트에서 인터럽트·타임아웃은 정상 상황이 아니므로 unchecked 로 바꿔 즉시 실패시킨다.
     */
    private static void awaitOrFail(CountDownLatch latch) {
        try {
            if (!latch.await(LATCH_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                throw new IllegalStateException("래치 대기 시간 초과 — T2 가 끝났다는 신호가 오지 않았다");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
