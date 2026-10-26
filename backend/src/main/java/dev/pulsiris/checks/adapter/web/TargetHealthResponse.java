package dev.pulsiris.checks.adapter.web;

import java.time.Instant;
import java.util.UUID;

import dev.pulsiris.checks.application.TargetHealth;
import dev.pulsiris.checks.domain.HealthState;
import dev.pulsiris.checks.domain.HealthStatus;

record TargetHealthResponse(UUID targetId, String name, boolean enabled, HealthStatus status,
		int consecutiveFailures, int consecutiveSuccesses, Instant lastCheckedAt, Instant failingSince,
		Instant lastStatusChangeAt) {

	static TargetHealthResponse from(TargetHealth health) {
		HealthState state = health.state();
		return new TargetHealthResponse(state.targetId(), health.target().name(), health.target().enabled(),
				state.status(), state.consecutiveFailures(), state.consecutiveSuccesses(), state.lastCheckedAt(),
				state.failingSince(), state.lastStatusChangeAt());
	}

}
