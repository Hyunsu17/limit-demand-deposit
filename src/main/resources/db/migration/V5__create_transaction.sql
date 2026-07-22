-- 거래 3종. 입금 프로세스: TRANS_RAW 선적재(전문 원본) → 검증/원장변경 → TRANS_HISTORY 확정.
-- 월입금누계는 별도 원장 없이 TRANS_HISTORY 실시간 SUM 으로 집계(2026-07-20 Q1 결정 B).

-- TRANS_CODE(거래코드 마스터) — transaction_history.txn_code 가 업무코드로 참조.
CREATE TABLE transaction_code
(
    txn_code_seq BIGSERIAL PRIMARY KEY,
    txn_code     VARCHAR(10)  NOT NULL, -- transaction_history 가 참조하는 업무코드
    txn_nm       VARCHAR(100) NOT NULL,
    dc_type      VARCHAR(20)  NOT NULL, -- CREDIT(입금)/DEBIT(출금)
    txn_type     VARCHAR(20)  NOT NULL, -- DEPOSIT/WITHDRAWAL/TRANSFER/INTEREST
    refund_yn    CHAR(1)      NOT NULL DEFAULT 'N',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_transaction_code_txn_code UNIQUE (txn_code)
);

-- 시드: 입금 + 한도초과 반송(2026-07-20 Q2 — 반송은 명시적 출금성 거래로 표현).
INSERT INTO transaction_code (txn_code, txn_nm, dc_type, txn_type, refund_yn)
VALUES ('DEP01', '입금', 'CREDIT', 'DEPOSIT', 'N'),
       ('RTN01', '입금한도초과반송', 'DEBIT', 'WITHDRAWAL', 'N');

-- TRANS_RAW(거래원본) — 성공/실패 무관하게 수신 전문을 선적재(부인방지·장애 재처리 근거).
-- raw_data(전문 원본)는 불변. process_status/process_dttm 만 처리 결과로 전이된다.
CREATE TABLE transaction_raw
(
    raw_seq        BIGSERIAL PRIMARY KEY,
    channel_type   VARCHAR(20)    NOT NULL, -- ATM/INTERNET/KAKAO/INTERBANK/BRANCH
    raw_dttm       TIMESTAMP      NOT NULL, -- 수신일시
    raw_data       TEXT           NOT NULL, -- 전문 원본 그대로
    acct_no        VARCHAR(20)    NOT NULL,
    txn_amt        NUMERIC(19, 4) NOT NULL,
    process_status VARCHAR(20)    NOT NULL, -- PENDING/COMPLETED/FAILED
    process_dttm   TIMESTAMP,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- 장애 추적: 특정 계좌의 수신 전문을 시간순 조회
CREATE INDEX idx_transaction_raw_acct_dttm ON transaction_raw (acct_no, raw_dttm);
-- 미처리(PENDING) 건 재처리 배치가 상태로 좁혀 조회 → 풀스캔 방지
CREATE INDEX idx_transaction_raw_status ON transaction_raw (process_status);

-- TRANS_HISTORY(확정 거래내역) — 원장 변경이 확정된 거래. 월입금누계 실시간 SUM 의 집계 대상.
-- org_ref_id(카톡이체 환불 역참조)는 카톡 스코프(2026-07-20 Q3 C)라 도입 시 별도 마이그레이션으로 추가.
CREATE TABLE transaction_history
(
    txn_seq       BIGSERIAL PRIMARY KEY,
    raw_seq       BIGINT,                      -- transaction_raw 참조(논리 FK)
    acct_no       VARCHAR(20)    NOT NULL,
    txn_code      VARCHAR(10)    NOT NULL,      -- transaction_code 참조(논리 FK)
    txn_dt        DATE           NOT NULL,
    txn_dttm      TIMESTAMP      NOT NULL,
    txn_amt       NUMERIC(19, 4) NOT NULL,
    balance_after NUMERIC(19, 4) NOT NULL,      -- 거래후잔액
    txn_status    VARCHAR(20)    NOT NULL,      -- NORMAL/CANCELLED/REFUNDED/FAILED
    channel_type  VARCHAR(20)    NOT NULL,
    description   VARCHAR(200),
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- 거래내역 조회는 항상 "특정 계좌의 특정 기간" 패턴 + 월누계 SUM 도 (acct_no, txn_dt) 범위
CREATE INDEX idx_transaction_history_acct_dt ON transaction_history (acct_no, txn_dt);
-- 원천-거래 연결 추적(장애 대응)
CREATE INDEX idx_transaction_history_raw ON transaction_history (raw_seq);
