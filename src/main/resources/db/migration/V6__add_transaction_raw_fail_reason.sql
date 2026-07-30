-- TRANS_RAW 실패사유 — 입금불능은 거절로 종결하고 반송 leg 를 만들지 않으므로(2026-07-28 결정),
-- 거절의 추적 근거가 TRANS_RAW 의 처리상태 + 실패사유밖에 없다. 그 자리를 만든다.
-- TRANS_HISTORY 에는 기록하지 않는다(고객 거래내역은 원장이 실제로 움직인 거래만).
ALTER TABLE transaction_raw
    ADD COLUMN fail_reason VARCHAR(30); -- 실패 시에만 채움. 정상/미처리는 NULL(process_status 가 표현)

COMMENT ON COLUMN transaction_raw.fail_reason IS
    'ProcessFailReason — ACCOUNT_NOT_FOUND/ACCOUNT_NOT_ACTIVE/DEPOSIT_LIMIT_EXCEEDED/SYSTEM_ERROR';

-- 처리상태와 실패사유가 어긋난 행(FAILED 인데 사유 없음 / 정상인데 사유 있음)을 DB 가 막는다.
-- 사유를 안 남긴 거절은 추적 근거가 사라지는 것이라 애플리케이션 규약에만 맡기지 않는다.
ALTER TABLE transaction_raw
    ADD CONSTRAINT ck_transaction_raw_fail_reason
        CHECK ( (process_status = 'FAILED' AND fail_reason IS NOT NULL)
            OR (process_status <> 'FAILED' AND fail_reason IS NULL) );
