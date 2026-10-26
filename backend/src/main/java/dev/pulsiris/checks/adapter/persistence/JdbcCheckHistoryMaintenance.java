package dev.pulsiris.checks.adapter.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import dev.pulsiris.checks.application.CheckHistoryMaintenance;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Plain SQL: aggregating and deleting millions of rows belongs in the database, not in
 * JPA, which works row by row.
 */
@Repository
class JdbcCheckHistoryMaintenance implements CheckHistoryMaintenance {

	// Re-summarizes from the latest summarized day onwards, so a run that was skipped or cut short
	// is caught up and a partially summarized day is completed.
	private static final String SUMMARIZE = """
			INSERT INTO checks.check_daily_summary
			    (target_id, day, checks_total, checks_failed, uptime_percent, latency_avg_ms, latency_p95_ms)
			SELECT target_id,
			       (checked_at AT TIME ZONE 'UTC')::date,
			       count(*),
			       count(*) FILTER (WHERE outcome <> 'SUCCESS'),
			       round(100.0 * count(*) FILTER (WHERE outcome = 'SUCCESS') / count(*), 2),
			       round(avg(latency_ms)),
			       round(percentile_cont(0.95) WITHIN GROUP (ORDER BY latency_ms))
			FROM checks.check_result
			WHERE checked_at < ?
			  AND checked_at >= coalesce(
			        (SELECT max(day) FROM checks.check_daily_summary)::timestamp AT TIME ZONE 'UTC',
			        '-infinity')
			GROUP BY target_id, (checked_at AT TIME ZONE 'UTC')::date
			ON CONFLICT (target_id, day) DO UPDATE
			SET checks_total   = excluded.checks_total,
			    checks_failed  = excluded.checks_failed,
			    uptime_percent = excluded.uptime_percent,
			    latency_avg_ms = excluded.latency_avg_ms,
			    latency_p95_ms = excluded.latency_p95_ms
			""";

	// Deleting by id in small batches keeps locks short and spreads the work for autovacuum.
	private static final String DELETE_RESULTS = """
			DELETE FROM checks.check_result
			WHERE id IN (SELECT id FROM checks.check_result WHERE checked_at < ? LIMIT ?)
			""";

	private static final String DELETE_SUMMARIES = "DELETE FROM checks.check_daily_summary WHERE day < ?";

	private final JdbcTemplate jdbc;

	JdbcCheckHistoryMaintenance(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public int summarizeDaysBefore(LocalDate today) {
		return jdbc.update(SUMMARIZE, today.atStartOfDay().atOffset(ZoneOffset.UTC));
	}

	@Override
	public int deleteResultsBefore(Instant cutoff, int limit) {
		return jdbc.update(DELETE_RESULTS, utc(cutoff), limit);
	}

	@Override
	public int deleteSummariesBefore(LocalDate cutoff) {
		return jdbc.update(DELETE_SUMMARIES, cutoff);
	}

	// An explicit UTC offset: java.sql.Timestamp would be shifted by the JVM's time zone.
	private static OffsetDateTime utc(Instant instant) {
		return instant.atOffset(ZoneOffset.UTC);
	}

}
