package dev.pulsiris.registry.domain;

/**
 * Raw input for registering a target. Optional fields are {@code null} when the policy
 * default should apply.
 */
public record TargetDraft(
		String name,
		String endpointUrl,
		HttpMethod httpMethod,
		Integer expectedStatus,
		Integer intervalSeconds,
		Integer timeoutMs,
		Integer failureThreshold,
		Integer recoveryThreshold,
		Boolean enabled) {
}
