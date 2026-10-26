package dev.pulsiris.registry.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import dev.pulsiris.shared.ValidationException;
import dev.pulsiris.shared.Violation;

/**
 * A service endpoint that Pulsiris monitors.
 */
public record Target(
		UUID id,
		String name,
		URI endpointUrl,
		HttpMethod httpMethod,
		Integer expectedStatus,
		Duration interval,
		Duration timeout,
		int failureThreshold,
		int recoveryThreshold,
		boolean enabled,
		Instant createdAt,
		Instant updatedAt) {

	public static final int MAX_NAME_LENGTH = 100;

	public static final int MAX_URL_LENGTH = 2048;

	private static final Set<String> SCHEMES = Set.of("http", "https");

	// UUIDv7: time-ordered, so new rows append to the end of the primary key index.
	private static final TimeBasedEpochGenerator IDS = Generators.timeBasedEpochGenerator();

	public Target {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(name, "name");
		Objects.requireNonNull(endpointUrl, "endpointUrl");
		Objects.requireNonNull(httpMethod, "httpMethod");
		Objects.requireNonNull(interval, "interval");
		Objects.requireNonNull(timeout, "timeout");
		Objects.requireNonNull(createdAt, "createdAt");
		Objects.requireNonNull(updatedAt, "updatedAt");
	}

	/**
	 * Validates a draft against the policy, fills in defaults and creates a new target.
	 * Collects every violation instead of stopping at the first one.
	 */
	public static Target register(TargetDraft draft, TargetPolicy policy, Instant now) {
		return build(IDS.generate(), draft, policy, now, now);
	}

	/**
	 * Replaces all settings with the draft, using the same rules as registration. Identity and
	 * creation time are kept.
	 */
	public Target update(TargetDraft draft, TargetPolicy policy, Instant now) {
		return build(id, draft, policy, createdAt, now);
	}

	private static Target build(UUID id, TargetDraft draft, TargetPolicy policy, Instant createdAt,
			Instant updatedAt) {
		var defaults = policy.defaults();
		var limits = policy.limits();
		var violations = new ArrayList<Violation>();

		String name = draft.name() == null ? "" : draft.name().strip();
		if (name.isEmpty()) {
			violations.add(new Violation("name", "must not be blank"));
		}
		else if (name.length() > MAX_NAME_LENGTH) {
			violations.add(new Violation("name", "must be at most " + MAX_NAME_LENGTH + " characters"));
		}

		URI endpointUrl = parseEndpointUrl(draft.endpointUrl(), violations);

		Integer expectedStatus = draft.expectedStatus();
		if (expectedStatus != null && (expectedStatus < 100 || expectedStatus > 599)) {
			violations.add(new Violation("expectedStatus", "must be between 100 and 599"));
		}

		Duration interval = draft.intervalSeconds() == null ? defaults.interval()
				: Duration.ofSeconds(draft.intervalSeconds());
		boolean intervalValid = checkRange("intervalSeconds", interval, limits.minInterval(), limits.maxInterval(),
				violations);

		Duration timeout = draft.timeoutMs() == null ? defaults.timeout() : Duration.ofMillis(draft.timeoutMs());
		boolean timeoutValid = checkRange("timeoutMs", timeout, limits.minTimeout(), limits.maxTimeout(), violations);
		if (intervalValid && timeoutValid && timeout.compareTo(interval) >= 0) {
			violations.add(new Violation("timeoutMs", "must be shorter than the check interval"));
		}

		int failureThreshold = orDefault(draft.failureThreshold(), defaults.failureThreshold());
		int recoveryThreshold = orDefault(draft.recoveryThreshold(), defaults.recoveryThreshold());
		checkThreshold("failureThreshold", failureThreshold, limits.maxThreshold(), violations);
		checkThreshold("recoveryThreshold", recoveryThreshold, limits.maxThreshold(), violations);

		if (!violations.isEmpty()) {
			throw new ValidationException(violations);
		}

		return new Target(id, name, endpointUrl,
				draft.httpMethod() == null ? defaults.httpMethod() : draft.httpMethod(), expectedStatus, interval,
				timeout, failureThreshold, recoveryThreshold, draft.enabled() == null || draft.enabled(), createdAt,
				updatedAt);
	}

	private static URI parseEndpointUrl(String value, List<Violation> violations) {
		if (value == null || value.isBlank()) {
			violations.add(new Violation("endpointUrl", "must not be blank"));
			return null;
		}
		if (value.length() > MAX_URL_LENGTH) {
			violations.add(new Violation("endpointUrl", "must be at most " + MAX_URL_LENGTH + " characters"));
			return null;
		}
		try {
			URI uri = new URI(value.strip());
			if (uri.getScheme() == null || !SCHEMES.contains(uri.getScheme().toLowerCase())) {
				violations.add(new Violation("endpointUrl", "must use http or https"));
			}
			else if (uri.getHost() == null) {
				violations.add(new Violation("endpointUrl", "must include a host"));
			}
			else if (uri.getUserInfo() != null) {
				violations.add(new Violation("endpointUrl", "must not contain credentials"));
			}
			return uri;
		}
		catch (URISyntaxException ex) {
			violations.add(new Violation("endpointUrl", "is not a valid URL"));
			return null;
		}
	}

	private static boolean checkRange(String field, Duration value, Duration min, Duration max,
			List<Violation> violations) {
		if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
			violations.add(new Violation(field, "must be between " + describe(min) + " and " + describe(max)));
			return false;
		}
		return true;
	}

	private static void checkThreshold(String field, int value, int max, List<Violation> violations) {
		if (value < 1 || value > max) {
			violations.add(new Violation(field, "must be between 1 and " + max));
		}
	}

	private static int orDefault(Integer value, int fallback) {
		return value == null ? fallback : value;
	}

	private static String describe(Duration duration) {
		if (duration.toMillisPart() != 0 || duration.isZero()) {
			return duration.toMillis() + "ms";
		}
		long seconds = duration.toSeconds();
		if (seconds % 3600 == 0) {
			return seconds / 3600 + "h";
		}
		return seconds % 60 == 0 ? seconds / 60 + "m" : seconds + "s";
	}

}
