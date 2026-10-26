package dev.pulsiris.checks;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import dev.pulsiris.IntegrationTest;
import dev.pulsiris.MutableClock;
import dev.pulsiris.TestClockConfiguration;
import dev.pulsiris.checks.application.RetentionJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The nightly retention job against real PostgreSQL, with check results inserted at exact times.
 */
@IntegrationTest
@Import(TestClockConfiguration.class)
class RetentionTests {

	@Autowired
	RetentionJob job;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	private UUID target;

	private LocalDate today;

	@BeforeEach
	void moveToAFreshDay() {
		// The context (and its database) is shared with other tests; jumping far ahead keeps this
		// test's days newer than anything summarized before.
		clock.advance(Duration.ofDays(500));
		today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
		target = UUID.randomUUID();
		jdbc.update("""
				INSERT INTO checks.target_health_state
				    (target_id, status, consecutive_failures, consecutive_successes, version)
				VALUES (?, 'UP', 0, 0, 0)
				""", target);
	}

	@Test
	void summarizesEachFinishedDay() {
		LocalDate yesterday = today.minusDays(1);
		result(yesterday, 8, "SUCCESS", 100);
		result(yesterday, 9, "SUCCESS", 200);
		result(yesterday, 10, "SUCCESS", 300);
		result(yesterday, 11, "FAILURE", 1000);
		result(today, 1, "SUCCESS", 50);

		job.run();

		Map<String, Object> summary = summary(yesterday);
		assertThat(summary).containsEntry("checks_total", 4)
			.containsEntry("checks_failed", 1)
			.containsEntry("uptime_percent", new BigDecimal("75.00"))
			.containsEntry("latency_avg_ms", 400)
			// p95 of 100, 200, 300, 1000 interpolates 85% of the way from 300 to 1000.
			.containsEntry("latency_p95_ms", 895);
		assertThat(summaryCount(today)).as("today is not finished yet").isZero();
	}

	@Test
	void catchesUpOnDaysMissedWhileTheJobDidNotRun() {
		result(today.minusDays(3), 12, "SUCCESS", 100);
		result(today.minusDays(2), 12, "SUCCESS", 100);
		result(today.minusDays(1), 12, "TIMEOUT", 5000);

		job.run();

		assertThat(summaryCount(today.minusDays(3))).isOne();
		assertThat(summaryCount(today.minusDays(2))).isOne();
		assertThat(summary(today.minusDays(1))).containsEntry("uptime_percent", new BigDecimal("0.00"));
	}

	@Test
	void runningTwiceGivesTheSameSummaries() {
		result(today.minusDays(1), 8, "SUCCESS", 120);
		result(today.minusDays(1), 9, "ERROR", 30);

		job.run();
		Map<String, Object> first = summary(today.minusDays(1));
		job.run();

		assertThat(summary(today.minusDays(1))).isEqualTo(first);
	}

	@Test
	void deletesRawResultsOlderThanThirtyDays() {
		result(today.minusDays(31), 12, "SUCCESS", 100);
		result(today.minusDays(29), 12, "SUCCESS", 100);

		job.run();

		assertThat(resultCount()).isOne();
	}

	@Test
	void deletesSummariesOlderThanThirteenMonths() {
		summaryRow(today.minusDays(400));
		summaryRow(today.minusDays(10));

		job.run();

		assertThat(summaryCount(today.minusDays(400))).isZero();
		assertThat(summaryCount(today.minusDays(10))).isOne();
	}

	private void result(LocalDate day, int hour, String outcome, int latencyMs) {
		Instant checkedAt = day.atTime(hour, 0).toInstant(ZoneOffset.UTC);
		jdbc.update("""
				INSERT INTO checks.check_result (id, target_id, checked_at, outcome, latency_ms)
				VALUES (nextval('checks.check_result_seq'), ?, ?, ?::checks.check_outcome, ?)
				""", target, checkedAt.atOffset(ZoneOffset.UTC), outcome, latencyMs);
	}

	private void summaryRow(LocalDate day) {
		jdbc.update("""
				INSERT INTO checks.check_daily_summary
				VALUES (?, ?, 1, 0, 100.00, 10, 10)
				""", target, day);
	}

	private Map<String, Object> summary(LocalDate day) {
		return jdbc.queryForMap("""
				SELECT checks_total, checks_failed, uptime_percent, latency_avg_ms, latency_p95_ms
				FROM checks.check_daily_summary WHERE target_id = ? AND day = ?
				""", target, day);
	}

	private int summaryCount(LocalDate day) {
		return jdbc.queryForObject("SELECT count(*) FROM checks.check_daily_summary WHERE target_id = ? AND day = ?",
				Integer.class, target, day);
	}

	private int resultCount() {
		return jdbc.queryForObject("SELECT count(*) FROM checks.check_result WHERE target_id = ?", Integer.class,
				target);
	}

}
