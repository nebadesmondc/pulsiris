package dev.pulsiris.checks.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The current health of one target, derived from its consecutive check results. Follows the
 * target health state machine in {@code docs/architecture/03-failure-to-incident.drawio}.
 *
 * @param failingSince when the current problem started (first failure of the streak, kept while
 * DOWN), or {@code null} while healthy
 * @param lastStatusChangeAt when the status last changed, or {@code null} if it never has
 */
public record HealthState(UUID targetId, HealthStatus status, int consecutiveFailures, int consecutiveSuccesses,
		Instant lastCheckedAt, Instant failingSince, Instant lastStatusChangeAt) {

	public HealthState {
		Objects.requireNonNull(targetId, "targetId");
		Objects.requireNonNull(status, "status");
	}

	public static HealthState unknown(UUID targetId) {
		return new HealthState(targetId, HealthStatus.UNKNOWN, 0, 0, null, null, null);
	}

	/**
	 * Applies one check result and returns the new state, plus the status change other modules
	 * should hear about, if any. Results older than the last applied one are ignored.
	 */
	public Evaluation apply(CheckResult result, Thresholds thresholds) {
		if (!targetId.equals(result.targetId())) {
			throw new IllegalArgumentException("Result belongs to another target");
		}
		if (lastCheckedAt != null && result.checkedAt().isBefore(lastCheckedAt)) {
			return new Evaluation(this, null);
		}
		return result.outcome().isSuccess() ? onSuccess(result.checkedAt(), thresholds)
				: onFailure(result.checkedAt(), thresholds);
	}

	private Evaluation onFailure(Instant at, Thresholds thresholds) {
		int failures = consecutiveFailures + 1;
		Instant since = failingSince != null ? failingSince : at;
		if (status == HealthStatus.DOWN) {
			// Already in an outage: keep counting, but there is nothing new to announce.
			return new Evaluation(next(HealthStatus.DOWN, failures, 0, at, since), null);
		}
		if (failures >= thresholds.failureThreshold()) {
			return new Evaluation(next(HealthStatus.DOWN, failures, 0, at, since), StatusChange.WENT_DOWN);
		}
		return new Evaluation(next(HealthStatus.FAILING, failures, 0, at, since), null);
	}

	private Evaluation onSuccess(Instant at, Thresholds thresholds) {
		int successes = consecutiveSuccesses + 1;
		if (status == HealthStatus.DOWN) {
			if (successes >= thresholds.recoveryThreshold()) {
				return new Evaluation(next(HealthStatus.UP, 0, successes, at, null), StatusChange.RECOVERED);
			}
			// One good check is not enough to end an outage: stay DOWN, and the outage keeps its start time.
			return new Evaluation(next(HealthStatus.DOWN, 0, successes, at, failingSince), null);
		}
		// A success outside an outage clears any failure streak (a blip, not an incident).
		return new Evaluation(next(HealthStatus.UP, 0, successes, at, null), null);
	}

	private HealthState next(HealthStatus newStatus, int failures, int successes, Instant checkedAt,
			Instant since) {
		Instant changedAt = newStatus == status ? lastStatusChangeAt : checkedAt;
		return new HealthState(targetId, newStatus, failures, successes, checkedAt, since, changedAt);
	}

	/**
	 * @param change the transition to announce, or {@code null} when nothing changed that matters
	 */
	public record Evaluation(HealthState state, StatusChange change) {

		public Optional<StatusChange> statusChange() {
			return Optional.ofNullable(change);
		}

	}

}
