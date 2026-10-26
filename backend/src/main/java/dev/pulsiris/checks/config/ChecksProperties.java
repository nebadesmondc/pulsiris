package dev.pulsiris.checks.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code pulsiris.checks.*}.
 *
 * @param tick how often the scheduler looks for due targets
 * @param maxConcurrentProbes upper bound on probes running at the same time
 * @param schedulerEnabled turns the scheduler off, e.g. in tests that trigger runs themselves
 */
@ConfigurationProperties("pulsiris.checks")
public record ChecksProperties(Duration tick, int maxConcurrentProbes, boolean schedulerEnabled) {
}
