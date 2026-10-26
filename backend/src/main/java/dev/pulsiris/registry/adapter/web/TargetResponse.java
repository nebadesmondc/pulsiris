package dev.pulsiris.registry.adapter.web;

import java.time.Instant;
import java.util.UUID;

import dev.pulsiris.registry.domain.HttpMethod;
import dev.pulsiris.registry.domain.Target;

record TargetResponse(
		UUID id,
		String name,
		String endpointUrl,
		HttpMethod httpMethod,
		Integer expectedStatus,
		long intervalSeconds,
		long timeoutMs,
		int failureThreshold,
		int recoveryThreshold,
		boolean enabled,
		Instant createdAt,
		Instant updatedAt) {

	static TargetResponse from(Target target) {
		return new TargetResponse(target.id(), target.name(), target.endpointUrl().toString(), target.httpMethod(),
				target.expectedStatus(), target.interval().toSeconds(), target.timeout().toMillis(),
				target.failureThreshold(), target.recoveryThreshold(), target.enabled(), target.createdAt(),
				target.updatedAt());
	}

}
