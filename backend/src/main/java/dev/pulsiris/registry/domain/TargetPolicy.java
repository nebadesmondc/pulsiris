package dev.pulsiris.registry.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * Defaults applied to new targets and the limits they must stay within. Values come from
 * configuration, so deployments can change them without code changes.
 */
public record TargetPolicy(Defaults defaults, Limits limits) {

	public TargetPolicy {
		Objects.requireNonNull(defaults, "defaults");
		Objects.requireNonNull(limits, "limits");
		require(within(defaults.interval(), limits.minInterval(), limits.maxInterval()),
				"default interval outside limits");
		require(within(defaults.timeout(), limits.minTimeout(), limits.maxTimeout()),
				"default timeout outside limits");
		require(defaults.timeout().compareTo(defaults.interval()) < 0, "default timeout must be shorter than default interval");
		require(within(defaults.failureThreshold(), limits.maxThreshold())
				&& within(defaults.recoveryThreshold(), limits.maxThreshold()), "default thresholds outside limits");
	}

	public record Defaults(HttpMethod httpMethod, Duration interval, Duration timeout, int failureThreshold,
			int recoveryThreshold) {
	}

	public record Limits(Duration minInterval, Duration maxInterval, Duration minTimeout, Duration maxTimeout,
			int maxThreshold) {
	}

	private static boolean within(Duration value, Duration min, Duration max) {
		return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
	}

	private static boolean within(int value, int max) {
		return value >= 1 && value <= max;
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException("Invalid target policy: " + message);
		}
	}

}
