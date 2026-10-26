package dev.pulsiris.checks.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * The result of probing a target once.
 *
 * @param httpStatus status code, or {@code null} when no response was received
 * @param errorMessage short description of what went wrong, or {@code null} on success
 */
public record CheckResult(UUID targetId, Instant checkedAt, CheckOutcome outcome, Integer httpStatus,
		Duration latency, String errorMessage) {

	public CheckResult {
		Objects.requireNonNull(targetId, "targetId");
		Objects.requireNonNull(checkedAt, "checkedAt");
		Objects.requireNonNull(outcome, "outcome");
		Objects.requireNonNull(latency, "latency");
	}

}
