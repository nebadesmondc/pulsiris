package dev.pulsiris.checks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import dev.pulsiris.checks.config.ChecksProperties;
import org.junit.jupiter.api.Test;

/**
 * The job's orchestration with a fake maintenance port: order, cutoffs and batching.
 */
class RetentionJobTest {

	private static final Instant NOW = Instant.parse("2026-11-02T02:30:00Z");

	private final FakeMaintenance maintenance = new FakeMaintenance();

	@Test
	void summarizesBeforeDeletingAndUsesTheConfiguredCutoffs() {
		job(10).run();

		assertThat(maintenance.calls).containsExactly("summarize before 2026-11-02",
				"delete results before 2026-10-03T02:30:00Z", "delete summaries before 2025-10-03");
	}

	@Test
	void keepsDeletingWhileBatchesComeBackFull() {
		maintenance.deletedPerBatch.addAll(List.of(2, 2, 1));

		RetentionJob.Report report = job(2).run();

		assertThat(report.deletedResults()).isEqualTo(5);
		assertThat(maintenance.calls).filteredOn(call -> call.startsWith("delete results")).hasSize(3);
	}

	@Test
	void rejectsRawRetentionShorterThanTwoDays() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new ChecksProperties.Retention(Duration.ofHours(12), Duration.ofDays(395), "", 10));
	}

	private RetentionJob job(int batchSize) {
		var retention = new ChecksProperties.Retention(Duration.ofDays(30), Duration.ofDays(395), "", batchSize);
		var properties = new ChecksProperties(Duration.ofSeconds(5), 50, false, retention);
		return new RetentionJob(maintenance, properties, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private static final class FakeMaintenance implements CheckHistoryMaintenance {

		private final List<String> calls = new ArrayList<>();

		private final Deque<Integer> deletedPerBatch = new ArrayDeque<>();

		@Override
		public int summarizeDaysBefore(LocalDate today) {
			calls.add("summarize before " + today);
			return 0;
		}

		@Override
		public int deleteResultsBefore(Instant cutoff, int limit) {
			calls.add("delete results before " + cutoff);
			return deletedPerBatch.isEmpty() ? 0 : deletedPerBatch.pop();
		}

		@Override
		public int deleteSummariesBefore(LocalDate cutoff) {
			calls.add("delete summaries before " + cutoff);
			return 0;
		}

	}

}
