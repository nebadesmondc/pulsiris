package dev.pulsiris.checks.domain;

import static dev.pulsiris.checks.domain.CheckOutcome.ERROR;
import static dev.pulsiris.checks.domain.CheckOutcome.FAILURE;
import static dev.pulsiris.checks.domain.CheckOutcome.SUCCESS;
import static dev.pulsiris.checks.domain.CheckOutcome.TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class HealthStateTest {

	private static final UUID TARGET = UUID.randomUUID();

	private static final Instant START = Instant.parse("2026-10-27T09:00:00Z");

	private static final Thresholds DEFAULT = new Thresholds(3, 2);

	private static final Duration INTERVAL = Duration.ofMinutes(1);

	private static final Duration STALE_AFTER = INTERVAL.multipliedBy(3);

	@Test
	void startsUnknown() {
		HealthState state = HealthState.unknown(TARGET);

		assertThat(state.status()).isEqualTo(HealthStatus.UNKNOWN);
		assertThat(state.lastCheckedAt()).isNull();
	}

	@Test
	void firstSuccessMakesTargetUpWithoutAnnouncingIt() {
		Run run = new Run().check(SUCCESS);

		assertThat(run.state.status()).isEqualTo(HealthStatus.UP);
		assertThat(run.changes).isEmpty();
	}

	@Test
	void failuresBelowThresholdAreOnlyFailing() {
		Run run = new Run().check(SUCCESS, FAILURE, FAILURE);

		assertThat(run.state.status()).isEqualTo(HealthStatus.FAILING);
		assertThat(run.state.consecutiveFailures()).isEqualTo(2);
		assertThat(run.changes).isEmpty();
	}

	@Test
	void thirdFailureInARowGoesDownOnceAndRemembersWhenTheProblemStarted() {
		Run run = new Run().check(SUCCESS, FAILURE, TIMEOUT, ERROR);

		assertThat(run.state.status()).isEqualTo(HealthStatus.DOWN);
		assertThat(run.changes).containsExactly(StatusChange.WENT_DOWN);
		assertThat(run.state.failingSince()).isEqualTo(run.timeOf(2));
		assertThat(run.state.lastStatusChangeAt()).isEqualTo(run.timeOf(4));
	}

	@Test
	void furtherFailuresDuringAnOutageAnnounceNothingNew() {
		Run run = new Run().check(FAILURE, FAILURE, FAILURE);
		run.check(FAILURE, FAILURE, FAILURE, FAILURE);

		assertThat(run.state.status()).isEqualTo(HealthStatus.DOWN);
		assertThat(run.state.consecutiveFailures()).isEqualTo(7);
		assertThat(run.state.failingSince()).isEqualTo(run.timeOf(1));
		assertThat(run.changes).containsExactly(StatusChange.WENT_DOWN);
	}

	@Test
	void successWhileFailingIsABlipNotAnIncident() {
		Run run = new Run().check(SUCCESS, FAILURE, FAILURE, SUCCESS);

		assertThat(run.state.status()).isEqualTo(HealthStatus.UP);
		assertThat(run.state.consecutiveFailures()).isZero();
		assertThat(run.state.failingSince()).isNull();
		assertThat(run.changes).isEmpty();
	}

	@Test
	void recoveryNeedsTheConfiguredNumberOfSuccessesInARow() {
		Run run = new Run().check(FAILURE, FAILURE, FAILURE, SUCCESS);

		assertThat(run.state.status()).isEqualTo(HealthStatus.DOWN);

		run.check(SUCCESS);

		assertThat(run.state.status()).isEqualTo(HealthStatus.UP);
		assertThat(run.state.failingSince()).isNull();
		assertThat(run.changes).containsExactly(StatusChange.WENT_DOWN, StatusChange.RECOVERED);
	}

	@Test
	void flappingDuringRecoveryKeepsTheOutageOpen() {
		Run run = new Run().check(FAILURE, FAILURE, FAILURE, SUCCESS, FAILURE);

		assertThat(run.state.status()).isEqualTo(HealthStatus.DOWN);
		assertThat(run.state.consecutiveSuccesses()).isZero();
		assertThat(run.state.failingSince()).isEqualTo(run.timeOf(1));
		assertThat(run.changes).containsExactly(StatusChange.WENT_DOWN);

		run.check(SUCCESS, SUCCESS);

		assertThat(run.changes).containsExactly(StatusChange.WENT_DOWN, StatusChange.RECOVERED);
	}

	@Test
	void thresholdOfOneReactsToASingleResult() {
		Run run = new Run(new Thresholds(1, 1)).check(SUCCESS, FAILURE);

		assertThat(run.state.status()).isEqualTo(HealthStatus.DOWN);

		run.check(SUCCESS);

		assertThat(run.changes).containsExactly(StatusChange.WENT_DOWN, StatusChange.RECOVERED);
	}

	@Test
	void ignoresResultsOlderThanTheLastOneApplied() {
		HealthState state = new Run().check(SUCCESS).state;
		CheckResult stale = result(FAILURE, START.minusSeconds(60));

		HealthState.Evaluation evaluation = state.apply(stale, DEFAULT, STALE_AFTER);

		assertThat(evaluation.state()).isEqualTo(state);
		assertThat(evaluation.statusChange()).isEmpty();
	}

	@Test
	void rejectsResultsOfAnotherTarget() {
		CheckResult foreign = new CheckResult(UUID.randomUUID(), START, SUCCESS, 200, Duration.ofMillis(5), null);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> HealthState.unknown(TARGET).apply(foreign, DEFAULT, STALE_AFTER));
	}

	@Test
	void failuresFromBeforeAGapDoNotCountTowardsANewOutage() {
		Run run = new Run().check(SUCCESS, FAILURE, FAILURE).pause(Duration.ofDays(2)).check(FAILURE);

		assertThat(run.state.status()).isEqualTo(HealthStatus.FAILING);
		assertThat(run.state.consecutiveFailures()).isEqualTo(1);
		assertThat(run.state.failingSince()).as("the outage starts after the gap").isEqualTo(run.timeOf(4));
		assertThat(run.changes).isEmpty();
	}

	@Test
	void shortDelaysAreNotGaps() {
		// Up to 3 intervals between checks still counts as continuous monitoring.
		Run run = new Run().check(FAILURE).pause(Duration.ofMinutes(1)).check(FAILURE).pause(Duration.ofMinutes(1));
		run.check(FAILURE);

		assertThat(run.state.status()).isEqualTo(HealthStatus.DOWN);
		assertThat(run.state.failingSince()).isEqualTo(run.timeOf(1));
	}

	@Test
	void aConfirmedOutageSurvivesAGapAndReportsTheUnwatchedTime() {
		Run run = new Run().check(FAILURE, FAILURE, FAILURE).pause(Duration.ofHours(2)).check(FAILURE);

		assertThat(run.state.status()).isEqualTo(HealthStatus.DOWN);
		assertThat(run.state.failingSince()).isEqualTo(run.timeOf(1));

		run.check(SUCCESS, SUCCESS);

		assertThat(run.changes).containsExactly(StatusChange.WENT_DOWN, StatusChange.RECOVERED);
		// From the last check before the gap to the first one after it.
		assertThat(run.unobservedOnRecovery).isEqualTo(Duration.ofHours(2).plus(INTERVAL));
		assertThat(run.state.unobserved()).as("reset once the outage ends").isZero();
	}

	@Test
	void aSuccessFromBeforeAGapDoesNotCountTowardsRecovery() {
		Run run = new Run().check(FAILURE, FAILURE, FAILURE, SUCCESS).pause(Duration.ofHours(1)).check(SUCCESS);

		assertThat(run.state.status()).isEqualTo(HealthStatus.DOWN);
		assertThat(run.state.consecutiveSuccesses()).isEqualTo(1);
	}

	private static CheckResult result(CheckOutcome outcome, Instant at) {
		return new CheckResult(TARGET, at, outcome, outcome == SUCCESS ? 200 : null, Duration.ofMillis(42), null);
	}

	/**
	 * Feeds results one minute apart (the interval) and records every announced status change and
	 * the unobserved time reported on recovery.
	 */
	private static final class Run {

		private final Thresholds thresholds;

		private final List<StatusChange> changes = new ArrayList<>();

		private final List<Instant> times = new ArrayList<>();

		private HealthState state = HealthState.unknown(TARGET);

		private Instant now = START;

		private Duration unobservedOnRecovery = Duration.ZERO;

		Run() {
			this(DEFAULT);
		}

		Run(Thresholds thresholds) {
			this.thresholds = thresholds;
		}

		Run check(CheckOutcome... outcomes) {
			for (CheckOutcome outcome : outcomes) {
				now = now.plus(INTERVAL);
				times.add(now);
				HealthState.Evaluation evaluation = state.apply(result(outcome, now), thresholds, STALE_AFTER);
				state = evaluation.state();
				evaluation.statusChange().ifPresent(changes::add);
				if (evaluation.unobservedDuringOutage().isPositive()) {
					unobservedOnRecovery = evaluation.unobservedDuringOutage();
				}
			}
			return this;
		}

		/** No checks for this long, e.g. while Pulsiris is offline. */
		Run pause(Duration duration) {
			now = now.plus(duration);
			return this;
		}

		/** Time of the n-th check (1-based). */
		Instant timeOf(int n) {
			return times.get(n - 1);
		}

	}

}
