package com.hyunsu.limitdeposit.product.domain;

import com.hyunsu.limitdeposit.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DEP_LMT_POLICY_MST — 입금한도정책.
 * ACCT_LEDGER.dep_lmt_policy_id 가 정책ID로 참조하며, 유효일자 구간(APPY_STT_DT <= 기준일 <= APPY_END_DT)으로
 * 현재 정책을 조회한다. monthly_dp_lmt_amt 가 월입금한도 판단(MonthlyDepositLimit)의 한도값 원천이다.
 * 기준정보는 Flyway 시드로 적재하며 애플리케이션에서 생성하지 않는다. (Product 와 동일 패턴)
 */
@Entity
@Table(name = "deposit_limit_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositLimitPolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dep_lmt_policy_seq")
    private Long id;

    @Column(name = "dep_lmt_policy_id", nullable = false, length = 3)
    private String depLmtPolicyId;

    @Column(name = "bal_lmt_amt", nullable = false, precision = 19, scale = 4)
    private BigDecimal balLmtAmt;

    @Column(name = "monthly_dp_lmt_amt", nullable = false, precision = 19, scale = 4)
    private BigDecimal monthlyDpLmtAmt;

    @Column(name = "appy_stt_dt", nullable = false)
    private LocalDate appySttDt;

    @Column(name = "appy_end_dt", nullable = false)
    private LocalDate appyEndDt;
}
