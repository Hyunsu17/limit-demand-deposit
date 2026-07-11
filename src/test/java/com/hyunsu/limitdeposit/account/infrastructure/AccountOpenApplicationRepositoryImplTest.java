package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.Channel;
import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplication;
import com.hyunsu.limitdeposit.account.domain.opening.AccountOpenApplicationRepository;
import com.hyunsu.limitdeposit.account.domain.opening.ApplicationStatus;
import com.hyunsu.limitdeposit.common.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D7-A 1차 방어 쿼리(existsByCustomerIdAndAppStatus)가 실제 DB에서
 * "고객 + 상태" 조합으로 정확히 필터링하는지 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AccountOpenApplicationRepositoryImpl.class, JpaConfig.class})
@ActiveProfiles("test")
class AccountOpenApplicationRepositoryImplTest {

    @Autowired
    private AccountOpenApplicationRepository accountOpenApplicationRepository;

    private static final Long CUSTOMER_ID = 1L;
    private static final String PROD_CD = "HNDEP001";

    private AccountOpenApplication pendingApplication(Long customerId) {
        return AccountOpenApplication.apply(customerId, PROD_CD, Channel.NON_FACE_TO_FACE);
    }

    @Test
    @DisplayName("진행중_PENDING_신청이_있으면_existsByCustomerIdAndAppStatus가_true를_반환한다")
    void existsByCustomerIdAndAppStatus_pending_true() {
        // given
        accountOpenApplicationRepository.save(pendingApplication(CUSTOMER_ID));

        // then
        assertThat(accountOpenApplicationRepository
                .existsByCustomerIdAndAppStatus(CUSTOMER_ID, ApplicationStatus.PENDING)).isTrue();
    }

    @Test
    @DisplayName("신청이_이미_종결되면_PENDING_조회는_false를_반환한다_상태필터_검증")
    void existsByCustomerIdAndAppStatus_completed_false() {
        // given — 반려로 종결된 신청만 존재
        AccountOpenApplication application = pendingApplication(CUSTOMER_ID);
        application.reject("테스트 반려");
        accountOpenApplicationRepository.save(application);

        // then
        assertThat(accountOpenApplicationRepository
                .existsByCustomerIdAndAppStatus(CUSTOMER_ID, ApplicationStatus.PENDING)).isFalse();
    }

    @Test
    @DisplayName("다른_고객의_PENDING_신청은_조회되지_않는다_고객필터_검증")
    void existsByCustomerIdAndAppStatus_otherCustomer_false() {
        // given
        accountOpenApplicationRepository.save(pendingApplication(999L));

        // then
        assertThat(accountOpenApplicationRepository
                .existsByCustomerIdAndAppStatus(CUSTOMER_ID, ApplicationStatus.PENDING)).isFalse();
    }
}
