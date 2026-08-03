package com.hyunsu.limitdeposit.transaction.infrastructure;

import com.hyunsu.limitdeposit.transaction.domain.ProcessStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V6 ck_transaction_raw_fail_reason — FAILED ⟺ fail_reason NOT NULL 제약을 실제 DB에서 검증한다.
 *
 * 검증 대상이 어댑터가 아니라 마이그레이션(제약 그 자체)이라 ...RepositoryImplTest 와 파일을 분리했다.
 *
 * 반송 없는 거절에서는 처리상태 + 실패사유가 거절의 유일한 추적 근거이므로,
 * 사유 없는 거절을 애플리케이션 규약에만 맡기지 않고 DB 제약으로 함께 막았다(2026-07-30 결정).
 * 이 테스트가 보는 것은 그 이중 방어의 바깥층 — 안쪽(도메인 메서드)이 뚫렸을 때 DB가 마지막 관문이 되는가.
 *
 * TransactionRaw.markFailed()가 사유를 필수 인자로 받아 엔티티로는 위반 상태를 만들 수 없다.
 * 위반 데이터는 도메인을 우회해 JdbcTemplate으로 직접 적재한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TransactionRawConstraintTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 이 제약이 보는 컬럼은 process_status 와 fail_reason 둘뿐이므로 그 두 축만 파라미터로 노출하고,
     * 나머지 NOT NULL 은 고정값으로 묻는다 — 각 테스트에서 "무엇이 다른가"만 눈에 남는다.
     *
     * failReason 에 null 을 넘기면 NULL 로 적재된다. 위반 케이스와 정상 케이스가 같은 헬퍼를 쓴다.
     * 문자열로 받는 이유: 검증 대상이 DB 에 실제로 들어간 값이라 enum 을 경유할 이유가 없다.
     */
    private void insertRaw(String processStatus, String failReason) {
        jdbcTemplate.update("""
            INSERT INTO transaction_raw (channel_type, raw_dttm, raw_data, acct_no, txn_amt,
                                         process_status, fail_reason)
            VALUES ('INTERBANK', NOW(), '{}', '1000000000001', 10000, ?, ?)
            """, processStatus, failReason);
    }

    // ── 거부돼야 하는 조합 ──────────────────────────────────────────

    @Test
    @DisplayName("처리실패인데_사유가_없으면_DB가_거부한다")
    void insert_failedWithoutReason_rejected() {
        assertThatThrownBy(()->insertRaw(ProcessStatus.FAILED.name(), null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("처리대기인데_사유가_있으면_DB가_거부한다")
    void insert_pendingWithReason_rejected() {
        assertThatThrownBy(()->insertRaw(ProcessStatus.PENDING.name(), "거부사유"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("처리완료인데_사유가_있으면_DB가_거부한다")
    void insert_completedWithReason_rejected() {
        assertThatThrownBy(()->insertRaw(ProcessStatus.COMPLETED.name(), "거부사유"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ── 통과해야 하는 조합 (제약이 너무 빡빡한 경우를 잡는다) ────────────

    @Test
    @DisplayName("처리실패에_사유가_있으면_정상_적재된다")
    void insert_failedWithReason_accepted() {
        assertThatCode(()->insertRaw(ProcessStatus.FAILED.name(), "거부사유"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("처리대기에_사유가_없으면_정상_적재된다")
    void insert_pendingWithoutReason_accepted() {
        assertThatCode(()->insertRaw(ProcessStatus.PENDING.name(), null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("처리완료에_사유가_없으면_정상_적재된다")
    void insert_completedWithoutReason_accepted() {
         assertThatCode(()->insertRaw(ProcessStatus.COMPLETED.name(), null))
                .doesNotThrowAnyException();
    }
}
