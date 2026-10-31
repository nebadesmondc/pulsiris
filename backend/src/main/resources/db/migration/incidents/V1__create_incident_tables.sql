CREATE SCHEMA incidents;

CREATE TYPE incidents.incident_status AS ENUM ('OPEN', 'ACKNOWLEDGED', 'RESOLVED');
CREATE TYPE incidents.incident_resolution AS ENUM ('RECOVERED', 'MANUAL', 'TARGET_REMOVED');
CREATE TYPE incidents.timeline_entry_type AS ENUM ('OPENED', 'ACKNOWLEDGED', 'NOTE', 'RESOLVED');

-- target_id refers to registry.monitored_target without a foreign key; target_name is a snapshot.
CREATE TABLE incidents.incident
(
    id              UUID                          NOT NULL,
    target_id       UUID                          NOT NULL,
    target_name     VARCHAR(100)                  NOT NULL,
    status          incidents.incident_status     NOT NULL,
    started_at      TIMESTAMP WITH TIME ZONE      NOT NULL,
    opened_at       TIMESTAMP WITH TIME ZONE      NOT NULL,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    resolved_at     TIMESTAMP WITH TIME ZONE,
    resolution      incidents.incident_resolution,
    last_error      TEXT,
    version         BIGINT                        NOT NULL,
    PRIMARY KEY (id),
    -- One incident per outage, even when TargetWentDown is delivered again.
    CONSTRAINT incident_outage_uq UNIQUE (target_id, started_at),
    CONSTRAINT incident_resolved_ck CHECK ((status = 'RESOLVED') = (resolved_at IS NOT NULL AND resolution IS NOT NULL))
);

-- At most one unresolved incident per target.
CREATE UNIQUE INDEX incident_active_target_uq ON incidents.incident (target_id) WHERE status <> 'RESOLVED';

CREATE INDEX incident_opened_at_idx ON incidents.incident (opened_at DESC);

CREATE SEQUENCE incidents.timeline_entry_seq INCREMENT BY 50;

CREATE TABLE incidents.incident_timeline_entry
(
    id          BIGINT                        NOT NULL,
    incident_id UUID                          NOT NULL REFERENCES incidents.incident ON DELETE CASCADE,
    occurred_at TIMESTAMP WITH TIME ZONE      NOT NULL,
    entry_type  incidents.timeline_entry_type NOT NULL,
    actor       VARCHAR(100)                  NOT NULL,
    message     TEXT,
    PRIMARY KEY (id)
);

CREATE INDEX incident_timeline_entry_incident_idx ON incidents.incident_timeline_entry (incident_id, occurred_at);
