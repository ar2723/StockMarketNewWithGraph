-- 지표 메타, 가격(시간봉/일봉), 수집 이력. 설계 설명은 docs/erd.md
CREATE TABLE indicator (
    id                  BIGSERIAL    PRIMARY KEY,
    code                VARCHAR(20)  NOT NULL UNIQUE,
    name                VARCHAR(100) NOT NULL,
    category            VARCHAR(20)  NOT NULL,
    currency            VARCHAR(10)  NOT NULL,
    source              VARCHAR(30),
    source_symbol       VARCHAR(50),
    enabled             BOOLEAN      NOT NULL DEFAULT FALSE,
    spike_threshold_pct NUMERIC(5, 2) NOT NULL DEFAULT 2.00,
    display_order       INT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_indicator_category CHECK (category IN ('INDEX', 'FX', 'COMMODITY', 'CRYPTO'))
);

CREATE TABLE indicator_price_hourly (
    indicator_id BIGINT         NOT NULL REFERENCES indicator (id),
    candle_start TIMESTAMPTZ    NOT NULL,
    open_price   NUMERIC(24, 8) NOT NULL,
    high_price   NUMERIC(24, 8) NOT NULL,
    low_price    NUMERIC(24, 8) NOT NULL,
    close_price  NUMERIC(24, 8) NOT NULL,
    volume       NUMERIC(30, 8),
    collected_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    PRIMARY KEY (indicator_id, candle_start)
);

CREATE TABLE indicator_price_daily (
    indicator_id BIGINT         NOT NULL REFERENCES indicator (id),
    trade_date   DATE           NOT NULL,
    open_price   NUMERIC(24, 8) NOT NULL,
    high_price   NUMERIC(24, 8) NOT NULL,
    low_price    NUMERIC(24, 8) NOT NULL,
    close_price  NUMERIC(24, 8) NOT NULL,
    volume       NUMERIC(30, 8),
    change_rate  NUMERIC(12, 6),
    complete     BOOLEAN        NOT NULL,
    collected_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    PRIMARY KEY (indicator_id, trade_date)
);

CREATE TABLE batch_job_run (
    id              BIGSERIAL   PRIMARY KEY,
    job_name        VARCHAR(50) NOT NULL,
    indicator_id    BIGINT      REFERENCES indicator (id),
    status          VARCHAR(20) NOT NULL,
    items_processed INT         NOT NULL DEFAULT 0,
    message         TEXT,
    started_at      TIMESTAMPTZ NOT NULL,
    finished_at     TIMESTAMPTZ,
    CONSTRAINT ck_batch_job_run_status CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX ix_batch_job_run_job_started ON batch_job_run (job_name, started_at DESC);
