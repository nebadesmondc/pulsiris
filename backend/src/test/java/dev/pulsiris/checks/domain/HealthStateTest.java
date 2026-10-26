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

		HealthState.Evaluation evaluation = state.apply(stale, DEFAULT);

		assertThat(evaluation.state()).isEqualTo(state);
		assertThat(evaluation.statusChange()).isEmpty();
	}

	@Test
	void rejectsResultsOfAnotherTarget() {
		CheckResult foreign = new CheckResult(UUID.randomUUID(), START, SUCCESS, 200, Duration.ofMillis(5), null);

		assertThatIllegalArgumentException().isThrownBy(() -> HealthState.unknown(TARGET).apply(foreign, DEFAULT));
	}

	private static CheckResult result(CheckOutcome outcome, Instant at) {
		return new CheckResult(TARGET, at, outcome, outcome == SUCCESS ? 200 : null, Duration.ofMillis(42), null);
	}

	/** Feeds results one minute apart and records every announced status change. */
	private static final class Run {

		private final Thresholds thresholds;

		private final List<StatusChange> changes = new ArrayList<>();

		private HealthState state = HealthState.unknown(TARGET);

		private int checks;

		Run() {
			this(DEFAULT);
		}

		Run(Thresholds thresholds) {
			this.thresholds = thresholds;
		}

		Run check(CheckOutcome... outcomes) {
			for (CheckOutcome outcome : outcomes) {
				checks++;
				HealthState.Evaluation evaluation = state.apply(result(outcome, timeOf(checks)), thresholds);
				state = evaluation.state();
				evaluation.statusChange().ifPresent(changes::add);
			}
			return this;
		}

		/** Time of the n-th check (1-based). */
		Instant timeOf(int n) {
			return START.plus(Duration.ofMinutes(n));
		}

	}

}
