package com.hyunsu.limitdeposit.account.domain.opening;

import com.hyunsu.limitdeposit.account.domain.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountOpenApplicationTest {

    private AccountOpenApplication application;

    @BeforeEach
    void setUp() {
        application = AccountOpenApplication.apply(1L, "PROD001", Channel.NON_FACE_TO_FACE);
    }

    @Test
    @DisplayName("PENDING_상태에서_approve하면_APPROVED로_전이되고_계좌번호가_채워진다")
    void approve_success() {
        // when
        application.approve("1234567890123");

        // then
        assertThat(application.getAppStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(application.getAcctNo()).isEqualTo("1234567890123");
        assertThat(application.getCompleteDttm()).isNotNull();
    }

    @Test
    @DisplayName("PENDING이_아닌_상태에서_approve하면_예외가_발생한다")
    void approve_notpending_exception_throws() {
        // given
        application.approve("1234567890123");

        // when & then
        assertThatThrownBy(() -> application.approve("9999999999999"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("PENDING_상태에서_reject하면_REJECT로_전이되고_거절사유가채워진다")
    void reject_success() {
        // when
        application.reject("테스트");

        // then
        assertThat(application.getAppStatus()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(application.getCompleteDttm()).isNotNull();
        assertThat(application.getRejectRsn()).isNotNull();
        assertThat(application.getAcctNo()).isNull();
    }

    @Test
    @DisplayName("PENDING이_아닌상태에서_REJECT하면_EXCPETION")
    void reject_notpending_expception_throws() {
        // given
        application.reject("첫번째");

        // when & then
        assertThatThrownBy(() -> application.reject("두번째"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("PENDING_상태에서_MARK_COMM_ERROR하면_동작")
    void markCommError_success() {
        // when
        application.markCommError();

        // then
        assertThat(application.getAppStatus()).isEqualTo(ApplicationStatus.COMM_ERROR);
        assertThat(application.getAcctNo()).isNull();
        assertThat(application.getCompleteDttm()).isNotNull();
    }

    @Test
    @DisplayName("PENDING_상태아닌경우_MARK_COMM_ERROR하면_EXCEPTION")
    void markCommError_notpending_exception_throws() {
        // given
        application.markCommError();

        // when & then
        assertThatThrownBy(() -> application.markCommError())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("PENDING_상태인경우_MARK_SYSTEM_ERROR하면_동작")
    void markSystemError_success() {
        // when
        application.markSystemError();

        // then
        assertThat(application.getAppStatus()).isEqualTo(ApplicationStatus.SYSTEM_ERROR);
        assertThat(application.getAcctNo()).isNull();
        assertThat(application.getCompleteDttm()).isNotNull();
    }

    @Test
    @DisplayName("PENDING_상태아닌경우_MARK_SYSTEM_ERROR하면_EXCEPTION")
    void markSystemError_notpending_exception_throws() {
        // given
        application.markSystemError();

        // when & then
        assertThatThrownBy(() -> application.markSystemError())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("IS_PENDING_APPLY_직후_TRUE_APPROVE_직후_FALSE")
    void isPending_transitions_correctly() {
        // then
        assertThat(application.isPending()).isEqualTo(true);

        // when
        application.approve("1234567890123");

        // then
        assertThat(application.isPending()).isEqualTo(false);
    }
}
