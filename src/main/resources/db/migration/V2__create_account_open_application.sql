CREATE TABLE account_open_application
(
    application_id     BIGSERIAL PRIMARY KEY,
    customer_id        BIGINT       NOT NULL,
    prod_cd            VARCHAR(10)  NOT NULL,
    application_dttm   TIMESTAMP    NOT NULL,
    application_channel VARCHAR(20) NOT NULL,
    app_status         VARCHAR(20)  NOT NULL,
    reject_rsn         VARCHAR(200),
    acct_no            VARCHAR(20),
    complete_dttm      TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE ncis_check_history
(
    ncis_check_id  BIGSERIAL PRIMARY KEY,
    application_id BIGINT      NOT NULL,
    req_dttm       TIMESTAMP   NOT NULL,
    res_dttm       TIMESTAMP,
    check_result   VARCHAR(20) NOT NULL,
    res_msg        VARCHAR(200),
    check_channel  VARCHAR(20),
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP   NOT NULL DEFAULT NOW()
);
