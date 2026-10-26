package dev.pulsiris.checks.domain;

/**
 * How many consecutive results change a target's status. Comes from the target's settings,
 * so it is passed in on every evaluation and may change between checks.
 */
public record Thresholds(int failureThreshold, int recoveryThreshold) {

	public Thresholds {
		if (failureThreshold < 1 || recoveryThreshold < 1) {
			throw new IllegalArgumentException("Thresholds must be at least 1");
		}
	}

}
