CREATE SCHEMA registry;

CREATE TYPE registry.http_method AS ENUM ('GET', 'HEAD');

CREATE TABLE registry.monitored_target
(
    id                 UUID                     NOT NULL,
    name               VARCHAR(100)             NOT NULL,
    endpoint_url       TEXT                     NOT NULL,
    http_method        registry.http_method     NOT NULL,
    expected_status    SMALLINT,
    interval_seconds   INT                      NOT NULL,
    timeout_ms         INT                      NOT NULL,
    failure_threshold  SMALLINT                 NOT NULL,
    recovery_threshold SMALLINT                 NOT NULL,
    enabled            BOOLEAN                  NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT                   NOT NULL,
    PRIMARY KEY (id),
    CHECK (expected_status BETWEEN 100 AND 599),
    CHECK (timeout_ms < interval_seconds * 1000)
);

CREATE UNIQUE INDEX monitored_target_name_uq ON registry.monitored_target (lower(name));
