package dev.pulsiris.checks.application;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Outbound port for bulk history housekeeping. Implementations work set-based in the database.
 */
public interface CheckHistoryMaintenance {

	/**
	 * Builds or refreshes daily summaries for every day before {@code today} (UTC) that has raw
	 * results and is not older than the latest summarized day. Safe to run repeatedly.
	 * @return the number of target-days written
	 */
	int summarizeDaysBefore(LocalDate today);

	/**
	 * Deletes at most {@code limit} raw results checked before {@code cutoff}.
	 * @return the number deleted; fewer than {@code limit} means nothing is left to delete
	 */
	int deleteResultsBefore(Instant cutoff, int limit);

	/** @return the number of summaries deleted */
	int deleteSummariesBefore(LocalDate cutoff);

}
