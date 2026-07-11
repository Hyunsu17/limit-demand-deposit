package com.hyunsu.limitdeposit.product.domain;

import com.hyunsu.limitdeposit.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PROD_MST — 상품기본 + 금리 이력.
 * 유효일자 구간(APPY_STT_DT <= 기준일 <= APPY_END_DT)으로 금리 변경 이력을 관리한다.
 * 기준정보는 Flyway 시드로 적재하며 애플리케이션에서 생성하지 않는다.
 */
@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prod_seq")
    private Long id;

    @Column(name = "prod_cd", nullable = false, length = 10)
    private String prodCd;

    @Column(name = "prod_nm", nullable = false, length = 100)
    private String prodNm;

    @Column(name = "base_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal baseRate;

    @Column(name = "base_dep_lmt_policy_id", nullable = false, length = 3)
    private String baseDepLmtPolicyId;

    @Column(name = "base_pym_lmt_policy_id", nullable = false, length = 3)
    private String basePymLmtPolicyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_type", nullable = false, length = 20)
    private SettlementType settlementType;

    @Column(name = "appy_stt_dt", nullable = false)
    private LocalDate appySttDt;

    @Column(name = "appy_end_dt")
    private LocalDate appyEndDt;
}
