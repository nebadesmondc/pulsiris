CREATE SCHEMA checks;

CREATE TYPE checks.health_status AS ENUM ('UNKNOWN', 'UP', 'FAILING', 'DOWN');
CREATE TYPE checks.check_outcome AS ENUM ('SUCCESS', 'FAILURE', 'TIMEOUT', 'ERROR');

-- One row per target; target_id refers to registry.monitored_target without a foreign key.
CREATE TABLE checks.target_health_state
(
    target_id             UUID                     NOT NULL,
    status                checks.health_status     NOT NULL,
    consecutive_failures  INT                      NOT NULL,
    consecutive_successes INT                      NOT NULL,
    last_checked_at       TIMESTAMP WITH TIME ZONE,
    failing_since         TIMESTAMP WITH TIME ZONE,
    last_status_change_at TIMESTAMP WITH TIME ZONE,
    version               BIGINT                   NOT NULL,
    PRIMARY KEY (target_id)
);

CREATE SEQUENCE checks.check_result_seq INCREMENT BY 50;

CREATE TABLE checks.check_result
(
    id            BIGINT                   NOT NULL,
    target_id     UUID                     NOT NULL REFERENCES checks.target_health_state ON DELETE CASCADE,
    checked_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    outcome       checks.check_outcome     NOT NULL,
    http_status   SMALLINT,
    latency_ms    INT                      NOT NULL,
    error_message TEXT,
    PRIMARY KEY (id)
);

CREATE INDEX check_result_target_checked_idx ON checks.check_result (target_id, checked_at DESC);
