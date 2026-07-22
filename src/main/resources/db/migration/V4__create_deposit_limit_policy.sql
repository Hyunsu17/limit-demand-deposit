-- DEP_LMT_POLICY_MST(입금한도정책) — ACCT_LEDGER.dep_lmt_policy_id 가 정책ID로 참조.
-- 원장은 정책ID만 들고, 거래 시 유효일자(appy_stt_dt <= 기준일 <= appy_end_dt)로 현재 정책을 조회한다.
-- monthly_dp_lmt_amt 가 MonthlyDepositLimit VO 의 한도값 원천이다.
CREATE TABLE deposit_limit_policy
(
    dep_lmt_policy_seq BIGSERIAL PRIMARY KEY,
    dep_lmt_policy_id  VARCHAR(3)     NOT NULL, -- 원장이 참조하는 정책ID (D01 등)
    bal_lmt_amt        NUMERIC(19, 4) NOT NULL, -- 계좌보관한도액
    monthly_dp_lmt_amt NUMERIC(19, 4) NOT NULL, -- 월입금한도액
    appy_stt_dt        DATE           NOT NULL,
    appy_end_dt        DATE           NOT NULL,
    created_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- 정책ID + 유효기간으로 현재 정책을 조회하는 패턴 → 복합 인덱스
CREATE INDEX idx_deposit_limit_policy_id_dt ON deposit_limit_policy (dep_lmt_policy_id, appy_stt_dt);

-- 시드: D01 — PROD_MST(product) 시드의 base_dep_lmt_policy_id='D01' 이 참조하는 기본 정책.
-- [Claude] bal_lmt_amt·monthly_dp_lmt_amt 는 임시값 — 실제 한도요구불 상품 스펙 금액으로 교체 필요.
INSERT INTO deposit_limit_policy (dep_lmt_policy_id, bal_lmt_amt, monthly_dp_lmt_amt, appy_stt_dt, appy_end_dt)
VALUES ('D01', 50000000.0000, 30000000.0000, DATE '2026-01-01', DATE '9999-12-31');
