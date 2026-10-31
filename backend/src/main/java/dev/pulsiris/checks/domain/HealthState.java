package dev.pulsiris.checks.domain;

import java.time.Duration;
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
 * @param unobserved how long the target went unchecked during the current outage
 */
public record HealthState(UUID targetId, HealthStatus status, int consecutiveFailures, int consecutiveSuccesses,
		Instant lastCheckedAt, Instant failingSince, Instant lastStatusChangeAt, Duration unobserved) {

	public HealthState {
		Objects.requireNonNull(targetId, "targetId");
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(unobserved, "unobserved");
	}

	public static HealthState unknown(UUID targetId) {
		return new HealthState(targetId, HealthStatus.UNKNOWN, 0, 0, null, null, null, Duration.ZERO);
	}

	/**
	 * A target is due when it has never been checked or its interval has passed since the last check.
	 */
	public boolean isDue(Duration interval, Instant now) {
		return lastCheckedAt == null || !now.isBefore(lastCheckedAt.plus(interval));
	}

	/**
	 * Applies one check result and returns the new state, plus the status change other modules
	 * should hear about, if any. Results older than the last applied one are ignored.
	 *
	 * @param staleAfter a gap since the last check longer than this means the target went unwatched
	 */
	public Evaluation apply(CheckResult result, Thresholds thresholds, Duration staleAfter) {
		if (!targetId.equals(result.targetId())) {
			throw new IllegalArgumentException("Result belongs to another target");
		}
		if (lastCheckedAt != null && result.checkedAt().isBefore(lastCheckedAt)) {
			return new Evaluation(this, null, Duration.ZERO);
		}
		HealthState base = isStale(result.checkedAt(), staleAfter) ? afterGap(result.checkedAt()) : this;
		return result.outcome().isSuccess() ? base.onSuccess(result.checkedAt(), thresholds)
				: base.onFailure(result.checkedAt(), thresholds);
	}

	private boolean isStale(Instant at, Duration staleAfter) {
		return lastCheckedAt != null && Duration.between(lastCheckedAt, at).compareTo(staleAfter) > 0;
	}

	/**
	 * Results from before a gap say nothing about now, so their streak is forgotten. A confirmed
	 * outage stays DOWN (its incident is open), and the time nobody watched is remembered.
	 */
	private HealthState afterGap(Instant at) {
		if (status == HealthStatus.DOWN) {
			return new HealthState(targetId, status, consecutiveFailures, 0, lastCheckedAt, failingSince,
					lastStatusChangeAt, unobserved.plus(Duration.between(lastCheckedAt, at)));
		}
		return new HealthState(targetId, status, 0, 0, lastCheckedAt, null, lastStatusChangeAt, Duration.ZERO);
	}

	private Evaluation onFailure(Instant at, Thresholds thresholds) {
		int failures = consecutiveFailures + 1;
		Instant since = failingSince != null ? failingSince : at;
		if (status == HealthStatus.DOWN) {
			// Already in an outage: keep counting, but there is nothing new to announce.
			return new Evaluation(next(HealthStatus.DOWN, failures, 0, at, since, unobserved), null, Duration.ZERO);
		}
		if (failures >= thresholds.failureThreshold()) {
			return new Evaluation(next(HealthStatus.DOWN, failures, 0, at, since, Duration.ZERO),
					StatusChange.WENT_DOWN, Duration.ZERO);
		}
		return new Evaluation(next(HealthStatus.FAILING, failures, 0, at, since, Duration.ZERO), null, Duration.ZERO);
	}

	private Evaluation onSuccess(Instant at, Thresholds thresholds) {
		int successes = consecutiveSuccesses + 1;
		if (status == HealthStatus.DOWN) {
			if (successes >= thresholds.recoveryThreshold()) {
				return new Evaluation(next(HealthStatus.UP, 0, successes, at, null, Duration.ZERO),
						StatusChange.RECOVERED, unobserved);
			}
			// One good check is not enough to end an outage: stay DOWN, and the outage keeps its start time.
			return new Evaluation(next(HealthStatus.DOWN, 0, successes, at, failingSince, unobserved), null,
					Duration.ZERO);
		}
		// A success outside an outage clears any failure streak (a blip, not an incident).
		return new Evaluation(next(HealthStatus.UP, 0, successes, at, null, Duration.ZERO), null, Duration.ZERO);
	}

	private HealthState next(HealthStatus newStatus, int failures, int successes, Instant checkedAt, Instant since,
			Duration unwatched) {
		Instant changedAt = newStatus == status ? lastStatusChangeAt : checkedAt;
		return new HealthState(targetId, newStatus, failures, successes, checkedAt, since, changedAt, unwatched);
	}

	/**
	 * @param change the transition to announce, or {@code null} when nothing changed that matters
	 * @param unobservedDuringOutage on recovery, how long the outage went unchecked; otherwise zero
	 */
	public record Evaluation(HealthState state, StatusChange change, Duration unobservedDuringOutage) {

		public Optional<StatusChange> statusChange() {
			return Optional.ofNullable(change);
		}

	}

}
