package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.account.Account;
import com.hyunsu.limitdeposit.account.domain.account.AccountRepository;
import com.hyunsu.limitdeposit.common.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AccountRepositoryImpl — 실제 Postgres(Testcontainers)에서만 검증 가능한 항목 전용.
 * 단순 위임(save/existsByCustomerId 자체)이 아니라, SQL 제약(D7-B UNIQUE)이 실제로 동작하는지가 목적.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AccountRepositoryImpl.class, JpaConfig.class})
@ActiveProfiles("test")
class AccountRepositoryImplTest {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountJpaRepository jpaRepository;

    private static final Long CUSTOMER_ID = 1L;
    private static final String PROD_CD = "HNDEP001";
    private static final Long NCIS_CHECK_ID = 1L;
    private static final String DEP_LMT_POLICY_ID = "D01";
    private static final String PYMT_LMT_POLICY_ID = "P01";

    private Account newAccount(String acctNo, Long customerId) {
        return Account.open(acctNo, customerId, PROD_CD, NCIS_CHECK_ID, DEP_LMT_POLICY_ID, PYMT_LMT_POLICY_ID);
    }

    @Test
    @DisplayName("저장한_계좌의_customerId는_existsByCustomerId로_조회된다")
    void save_and_existsByCustomerId_true() {
        // when
        accountRepository.save(newAccount("1000000000001", CUSTOMER_ID));

        // then
        assertThat(accountRepository.existsByCustomerId(CUSTOMER_ID)).isTrue();
    }

    @Test
    @DisplayName("저장된_적_없는_customerId는_existsByCustomerId가_false를_반환한다")
    void existsByCustomerId_false_when_not_saved() {
        // then
        assertThat(accountRepository.existsByCustomerId(CUSTOMER_ID)).isFalse();
    }

    @Test
    @DisplayName("D7-B_동일_customerId로_두_번째_계좌를_저장하면_UNIQUE_제약위반이_발생한다")
    void save_duplicateCustomerId_throws_DataIntegrityViolationException() {
        // given
        // [Claude] flush를 리포지토리 프록시(jpaRepository)를 통해 나가게 해야
        // [Claude] Spring의 예외 변환(PersistenceExceptionTranslationInterceptor)이 적용되어
        // [Claude] 진짜 DataIntegrityViolationException으로 잡힌다 (EntityManager.flush() 직접 호출은 변환 안 됨)
        jpaRepository.saveAndFlush(newAccount("1000000000001", CUSTOMER_ID));

        // when & then
        assertThatThrownBy(() -> jpaRepository.saveAndFlush(newAccount("1000000000002", CUSTOMER_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
