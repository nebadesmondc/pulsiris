package dev.pulsiris.checks.domain;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Everything needed to probe a target once.
 *
 * @param expectedStatus exact status that counts as healthy, or {@code null} to accept any 2xx
 */
public record ProbeTarget(UUID targetId, URI endpointUrl, ProbeMethod method, Integer expectedStatus,
		Duration timeout) {

	public ProbeTarget {
		Objects.requireNonNull(targetId, "targetId");
		Objects.requireNonNull(endpointUrl, "endpointUrl");
		Objects.requireNonNull(method, "method");
		Objects.requireNonNull(timeout, "timeout");
	}

	public boolean accepts(int status) {
		return expectedStatus == null ? status >= 200 && status < 300 : status == expectedStatus;
	}

}
