-- Time a target went unchecked during its current outage, e.g. while Pulsiris was offline.
ALTER TABLE checks.target_health_state
    ADD COLUMN unobserved_seconds BIGINT NOT NULL DEFAULT 0;
