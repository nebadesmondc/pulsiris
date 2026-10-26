package dev.pulsiris.checks.domain;

public enum HealthStatus {
	/** Not checked yet. */
	UNKNOWN,
	/** Healthy. */
	UP,
	/** Failing, but not often enough in a row to count as an outage. */
	FAILING,
	/** Failed the configured number of checks in a row. */
	DOWN
}
