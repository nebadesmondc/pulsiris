package dev.pulsiris.checks.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code pulsiris.checks.*}.
 *
 * @param tick how often the scheduler looks for due targets
 * @param maxConcurrentProbes upper bound on probes running at the same time
 * @param schedulerEnabled turns the schedulers off, e.g. in tests that trigger runs themselves
 * @param staleAfterIntervals a target unchecked for more than this many intervals starts a fresh
 * streak, since older results say nothing about now
 */
@ConfigurationProperties("pulsiris.checks")
public record ChecksProperties(Duration tick, int maxConcurrentProbes, boolean schedulerEnabled,
		int staleAfterIntervals, Retention retention) {

	public ChecksProperties {
		// Normal checks arrive one interval plus at most a tick apart; 2 keeps that from counting as a gap.
		if (staleAfterIntervals < 2) {
			throw new IllegalArgumentException("pulsiris.checks.stale-after-intervals must be at least 2");
		}
	}

	/**
	 * @param rawResults how long individual check results are kept
	 * @param summaries how long daily summaries are kept
	 * @param cron when the nightly job runs (UTC)
	 * @param batchSize raw results deleted per statement
	 */
	public record Retention(Duration rawResults, Duration summaries, String cron, int batchSize) {

		public Retention {
			// Raw results of the current day are summarized the next night, so they must outlive it.
			if (rawResults.compareTo(Duration.ofDays(2)) < 0) {
				throw new IllegalArgumentException("pulsiris.checks.retention.raw-results must be at least 2d");
			}
			if (batchSize < 1) {
				throw new IllegalArgumentException("pulsiris.checks.retention.batch-size must be positive");
			}
		}

	}

}
