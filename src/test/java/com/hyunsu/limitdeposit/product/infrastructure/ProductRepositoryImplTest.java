package com.hyunsu.limitdeposit.product.infrastructure;

import com.hyunsu.limitdeposit.common.config.JpaConfig;
import com.hyunsu.limitdeposit.product.domain.Product;
import com.hyunsu.limitdeposit.product.domain.ProductRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * findEffective — 유효일자 구간(APPY_STT_DT <= 기준일 <= APPY_END_DT, NULL이면 무기한) 쿼리의
 * 경계값을 실제 DB에서 검증한다. 금리 변경 이력 관리(구간별 계산)의 토대가 되는 쿼리.
 *
 * 기준정보는 Flyway 시드(V3: HNDEP001, 2026-01-01 ~ NULL)를 사용하고,
 * 종료일이 있는 구간은 Product에 생성 API가 없으므로(시드 전용 엔티티) SQL로 직접 적재한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ProductRepositoryImpl.class, JpaConfig.class})
@ActiveProfiles("test")
class ProductRepositoryImplTest {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String SEED_PROD_CD = "HNDEP001";           // V3 시드: 2026-01-01 ~ 무기한
    private static final LocalDate SEED_START_DATE = LocalDate.of(2026, 1, 1);

    private static final String CLOSED_PROD_CD = "TESTPROD";         // 테스트 전용: 2026-01-01 ~ 2026-06-30
    private static final LocalDate CLOSED_END_DATE = LocalDate.of(2026, 6, 30);

    private void insertClosedPeriodProduct() {
        jdbcTemplate.update("""
                INSERT INTO product (prod_cd, prod_nm, base_rate, base_dep_lmt_policy_id, base_pym_lmt_policy_id,
                                     settlement_type, appy_stt_dt, appy_end_dt)
                VALUES (?, '종료일있는상품', 0.0020, 'D01', 'P01', 'FOURTH_FRIDAY', ?, ?)
                """, CLOSED_PROD_CD, SEED_START_DATE, CLOSED_END_DATE);
    }

    @Test
    @DisplayName("유효기간_내_기준일이면_시드_상품이_조회되고_금리와_정책ID가_실려온다")
    void findEffective_withinPeriod_found() {
        // when
        Optional<Product> found = productRepository.findEffective(SEED_PROD_CD, LocalDate.of(2026, 7, 11));

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getProdCd()).isEqualTo(SEED_PROD_CD);
        assertThat(found.get().getBaseRate()).isEqualByComparingTo(new BigDecimal("0.0010"));
        assertThat(found.get().getBaseDepLmtPolicyId()).isEqualTo("D01");
        assertThat(found.get().getBasePymLmtPolicyId()).isEqualTo("P01");
    }

    @Test
    @DisplayName("기준일이_적용시작일과_같으면_조회된다_시작일_경계값")
    void findEffective_onStartDate_found() {
        assertThat(productRepository.findEffective(SEED_PROD_CD, SEED_START_DATE)).isPresent();
    }

    @Test
    @DisplayName("기준일이_적용시작일_전날이면_조회되지_않는다")
    void findEffective_beforeStartDate_empty() {
        assertThat(productRepository.findEffective(SEED_PROD_CD, SEED_START_DATE.minusDays(1))).isEmpty();
    }

    @Test
    @DisplayName("기준일이_적용종료일과_같으면_조회된다_종료일_경계값")
    void findEffective_onEndDate_found() {
        // given
        insertClosedPeriodProduct();

        // then
        assertThat(productRepository.findEffective(CLOSED_PROD_CD, CLOSED_END_DATE)).isPresent();
    }

    @Test
    @DisplayName("기준일이_적용종료일_다음날이면_조회되지_않는다")
    void findEffective_afterEndDate_empty() {
        // given
        insertClosedPeriodProduct();

        // then
        assertThat(productRepository.findEffective(CLOSED_PROD_CD, CLOSED_END_DATE.plusDays(1))).isEmpty();
    }

    @Test
    @DisplayName("존재하지_않는_상품코드는_빈_Optional을_반환한다")
    void findEffective_unknownProdCd_empty() {
        assertThat(productRepository.findEffective("UNKNOWN", LocalDate.of(2026, 7, 11))).isEmpty();
    }
}
