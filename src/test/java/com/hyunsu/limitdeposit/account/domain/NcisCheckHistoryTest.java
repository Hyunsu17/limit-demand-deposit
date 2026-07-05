package com.hyunsu.limitdeposit.account.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NcisCheckHistoryTest {

    private static final Long APPLICATION_ID = 1L;

    private NcisCheckHistory ncisCheckHistory;

    @BeforeEach
    void setUp() {
        ncisCheckHistory = NcisCheckHistory.preRecord(APPLICATION_ID, Channel.NON_FACE_TO_FACE);
    }

    @Test
    @DisplayName("선적재_직후에는_PROCESSING_상태이고_요청일시만_있고_응답일시와_응답메시지는_없어야_한다")
    void preRecord_success() {
        // then
        // [Claude] 선적재 패턴(D4)의 불변식 — "요청은 했지만 아직 응답받지 않은" 상태여야 한다
        assertThat(ncisCheckHistory.getCheckResult()).isEqualTo(NcisCheckResult.PROCESSING);
        assertThat(ncisCheckHistory.getReqDttm()).isNotNull();
        assertThat(ncisCheckHistory.getResDttm()).isNull();
        assertThat(ncisCheckHistory.getResMsg()).isNull();
        assertThat(ncisCheckHistory.getApplicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    @DisplayName("complete하면_응답결과와_응답일시_응답메시지가_반영된다")
    void complete_success() {
        // when
        ncisCheckHistory.complete(NcisCheckResult.APPROVED, "중복계좌 없음");

        // then
        assertThat(ncisCheckHistory.getCheckResult()).isEqualTo(NcisCheckResult.APPROVED);
        assertThat(ncisCheckHistory.getResDttm()).isNotNull();
        assertThat(ncisCheckHistory.getResMsg()).isEqualTo("중복계좌 없음");
    }

    @Test
    @DisplayName("PROCESSING을_응답결과로_complete하면_예외가_발생한다")
    void complete_processing_exception_throws() {
        // when & then
        assertThatThrownBy(() -> ncisCheckHistory.complete(NcisCheckResult.PROCESSING, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PROCESSING");
    }

    @Test
    @DisplayName("이미_완료된_이력을_다시_complete하면_예외가_발생한다")
    void complete_already_completed_exception_throws() {
        // given
        ncisCheckHistory.complete(NcisCheckResult.REJECTED, "중복계좌 존재");

        // when & then
        assertThatThrownBy(() -> ncisCheckHistory.complete(NcisCheckResult.APPROVED, "두번째"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REJECTED");
    }
}
