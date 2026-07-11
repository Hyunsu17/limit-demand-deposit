-- D5: 계좌번호 채번 시퀀스 — 순수 13자리 숫자 (LPAD로 포맷)
CREATE SEQUENCE acct_no_seq START 1000000000000 INCREMENT 1;

-- PROD_MST: 상품기본 + 금리 이력 (유효일자 구간으로 금리 변경 이력 관리)
CREATE TABLE product
(
    prod_seq               BIGSERIAL PRIMARY KEY,
    prod_cd                VARCHAR(10)   NOT NULL,
    prod_nm                VARCHAR(100)  NOT NULL,
    base_rate              NUMERIC(7, 4) NOT NULL,
    base_dep_lmt_policy_id VARCHAR(3)    NOT NULL,
    base_pym_lmt_policy_id VARCHAR(3)    NOT NULL,
    settlement_type        VARCHAR(20)   NOT NULL,
    appy_stt_dt            DATE          NOT NULL,
    appy_end_dt            DATE,
    created_at             TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ACCT_LEDGER: 계좌원장
-- 금액 컬럼은 프로젝트 규칙(NUMERIC(19,4)) 적용 — ERD의 DECIMAL(18,2)보다 CLAUDE.md 금액 원칙 우선
CREATE TABLE account_ledger
(
    acct_no            VARCHAR(20) PRIMARY KEY,
    customer_id        BIGINT         NOT NULL,
    prod_cd            VARCHAR(10)    NOT NULL,
    ncis_check_id      BIGINT,
    dep_lmt_policy_id  VARCHAR(3)     NOT NULL,
    pymt_lmt_policy_id VARCHAR(3)     NOT NULL,
    tax_type           VARCHAR(20)    NOT NULL,
    ntax_limt_amt      NUMERIC(19, 4),
    acct_status        VARCHAR(20)    NOT NULL,
    balance            NUMERIC(19, 4) NOT NULL,
    available_balance  NUMERIC(19, 4) NOT NULL,
    auto_trans_link_yn CHAR(1)        NOT NULL DEFAULT 'N',
    card_link_yn       CHAR(1)        NOT NULL DEFAULT 'N',
    loan_link_yn       CHAR(1)        NOT NULL DEFAULT 'N',
    other_prod_link_yn CHAR(1)        NOT NULL DEFAULT 'N',
    currency_code      CHAR(3)        NOT NULL DEFAULT 'KRW',
    open_dt            DATE           NOT NULL,
    close_dt           DATE,
    last_txn_dt        DATE,
    branch_code        VARCHAR(10),
    created_at         TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP      NOT NULL DEFAULT NOW(),

    -- D7-B: 1인1계좌 DB 최후 안전망 — 동시 TX2 충돌 시 DB가 최종 차단.
    -- UNIQUE 인덱스가 CUSTOMER_ID 조회 인덱스를 겸한다 (별도 IDX 불필요)
    CONSTRAINT uk_account_ledger_customer_id UNIQUE (customer_id)
);

-- ACCT_LEDGER_HIST: 원장 변경 이력 (개설 시 최초 이력 INSERT, 이후 변경 시 APPY_END_DT 마감 + 새 행)
CREATE TABLE account_ledger_history
(
    hist_id           BIGSERIAL PRIMARY KEY,
    acct_no           VARCHAR(20) NOT NULL,
    chg_type          VARCHAR(30) NOT NULL,
    chg_rsn           VARCHAR(200),
    chg_chnl          VARCHAR(20),
    appy_stt_dt       DATE        NOT NULL,
    appy_end_dt       DATE,
    prod_cd           VARCHAR(10) NOT NULL,
    dep_lmt_policy_id VARCHAR(3)  NOT NULL,
    pymt_lmt_policy_id VARCHAR(3) NOT NULL,
    tax_type          VARCHAR(20) NOT NULL,
    acct_status       VARCHAR(20) NOT NULL,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- "이 계좌가 특정 시점에 어떤 상태였는지" 패턴: 계좌번호로 좁히고 날짜 범위 필터
CREATE INDEX idx_account_ledger_history_acct_dt ON account_ledger_history (acct_no, appy_stt_dt);

-- 기준정보 시드: 한도요구불 상품 1건 (연 0.10%, 넷째 금요일 결산 — 상품설명서 기준)
INSERT INTO product (prod_cd, prod_nm, base_rate, base_dep_lmt_policy_id, base_pym_lmt_policy_id,
                     settlement_type, appy_stt_dt, appy_end_dt)
VALUES ('HNDEP001', '한도요구불예금', 0.0010, 'D01', 'P01', 'FOURTH_FRIDAY', DATE '2026-01-01', NULL);
