-- One row per target per UTC day, built from check_result by the nightly retention job.
CREATE TABLE checks.check_daily_summary
(
    target_id      UUID         NOT NULL REFERENCES checks.target_health_state ON DELETE CASCADE,
    day            DATE         NOT NULL,
    checks_total   INT          NOT NULL,
    checks_failed  INT          NOT NULL,
    uptime_percent NUMERIC(5,2) NOT NULL,
    latency_avg_ms INT          NOT NULL,
    latency_p95_ms INT          NOT NULL,
    PRIMARY KEY (target_id, day)
);

-- Lets the retention job find expired raw results without scanning the whole table.
CREATE INDEX check_result_checked_at_idx ON checks.check_result (checked_at);
