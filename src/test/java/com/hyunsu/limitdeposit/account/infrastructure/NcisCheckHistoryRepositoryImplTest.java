package com.hyunsu.limitdeposit.account.infrastructure;

import com.hyunsu.limitdeposit.account.domain.Channel;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckHistory;
import com.hyunsu.limitdeposit.account.domain.ncis.NcisCheckHistoryRepository;
import com.hyunsu.limitdeposit.common.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TX2가 의존하는 findByApplicationId(선적재 이력 조회) 커스텀 쿼리 검증.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NcisCheckHistoryRepositoryImpl.class, JpaConfig.class})
@ActiveProfiles("test")
class NcisCheckHistoryRepositoryImplTest {

    @Autowired
    private NcisCheckHistoryRepository ncisCheckHistoryRepository;

    private static final Long APPLICATION_ID = 100L;

    @Test
    @DisplayName("선적재한_이력은_applicationId로_조회된다")
    void findByApplicationId_found() {
        // given
        NcisCheckHistory saved = ncisCheckHistoryRepository
                .save(NcisCheckHistory.preRecord(APPLICATION_ID, Channel.NON_FACE_TO_FACE));

        // when
        Optional<NcisCheckHistory> found = ncisCheckHistoryRepository.findByApplicationId(APPLICATION_ID);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getApplicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    @DisplayName("선적재된_적_없는_applicationId는_빈_Optional을_반환한다")
    void findByApplicationId_notFound_empty() {
        // then
        assertThat(ncisCheckHistoryRepository.findByApplicationId(APPLICATION_ID)).isEmpty();
    }
}
