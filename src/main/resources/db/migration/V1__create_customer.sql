CREATE TABLE customer
(
    customer_id BIGSERIAL PRIMARY KEY,
    login_id    VARCHAR(50)  UNIQUE NOT NULL,
    password    VARCHAR(255)        NOT NULL,
    name        VARCHAR(50)         NOT NULL,
    birth_date  DATE                NOT NULL,
    created_at  TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP           NOT NULL DEFAULT NOW()
);
