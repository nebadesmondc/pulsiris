package dev.pulsiris.registry;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/**
 * Read-only view of a target's check settings, for other modules.
 *
 * @param httpMethod {@code GET} or {@code HEAD}
 * @param expectedStatus exact status that counts as healthy, or {@code null} for any 2xx
 */
public record TargetSnapshot(UUID id, String name, URI endpointUrl, String httpMethod, Integer expectedStatus,
		Duration interval, Duration timeout, int failureThreshold, int recoveryThreshold) {
}
