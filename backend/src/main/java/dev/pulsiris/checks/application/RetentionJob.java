package dev.pulsiris.checks.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import dev.pulsiris.checks.config.ChecksProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/**
 * Nightly housekeeping: summarize finished days, then delete raw results and summaries past their
 * retention. Summarizing first guarantees nothing is deleted before it has been summarized.
 */
@Service
public class RetentionJob {

	private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

	private final CheckHistoryMaintenance maintenance;

	private final ChecksProperties.Retention retention;

	private final Clock clock;

	RetentionJob(CheckHistoryMaintenance maintenance, ChecksProperties properties, Clock clock) {
		this.maintenance = maintenance;
		this.retention = properties.retention();
		this.clock = clock;
	}

	public Report run() {
		Instant now = clock.instant();
		LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

		int summarized = maintenance.summarizeDaysBefore(today);

		// Each batch commits on its own, so a long cleanup never holds locks for long.
		Instant rawCutoff = now.minus(retention.rawResults());
		int deletedResults = 0;
		int batch;
		do {
			batch = maintenance.deleteResultsBefore(rawCutoff, retention.batchSize());
			deletedResults += batch;
		}
		while (batch == retention.batchSize());

		int deletedSummaries = maintenance.deleteSummariesBefore(today.minusDays(retention.summaries().toDays()));

		Report report = new Report(summarized, deletedResults, deletedSummaries);
		log.info("Retention: summarized {} target-day(s), deleted {} check result(s) and {} summary row(s)",
				report.summarized(), report.deletedResults(), report.deletedSummaries());
		return report;
	}

	public record Report(int summarized, int deletedResults, int deletedSummaries) {
	}

}
