package dev.pulsiris.incidents.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

import dev.pulsiris.shared.ValidationException;
import dev.pulsiris.shared.Violation;

/**
 * One outage of one target, from detection to resolution. Follows the incident state machine in
 * {@code docs/architecture/03-failure-to-incident.drawio}: OPEN → ACKNOWLEDGED → RESOLVED, and a
 * resolved incident never reopens.
 *
 * @param startedAt when the outage started (the target's first failed check)
 * @param openedAt when the outage was confirmed and the incident opened
 * @param lastError what the confirming check reported
 */
public record Incident(
		UUID id,
		UUID targetId,
		String targetName,
		IncidentStatus status,
		Instant startedAt,
		Instant openedAt,
		Instant acknowledgedAt,
		Instant resolvedAt,
		Resolution resolution,
		String lastError) {

	/** The actor recorded for actions Pulsiris takes on its own. */
	public static final String SYSTEM_ACTOR = "pulsiris";

	public static final int MAX_ACTOR_LENGTH = 100;

	public static final int MAX_TEXT_LENGTH = 2000;

	private static final TimeBasedEpochGenerator IDS = Generators.timeBasedEpochGenerator();

	public Incident {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(targetId, "targetId");
		Objects.requireNonNull(targetName, "targetName");
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(startedAt, "startedAt");
		Objects.requireNonNull(openedAt, "openedAt");
	}

	public static Change open(UUID targetId, String targetName, Instant startedAt, Instant detectedAt,
			String lastError) {
		var incident = new Incident(IDS.generate(), targetId, targetName, IncidentStatus.OPEN, startedAt, detectedAt,
				null, null, null, lastError);
		return incident.recorded(incident, TimelineEntryType.OPENED, SYSTEM_ACTOR, lastError, detectedAt);
	}

	public Change acknowledge(String actor, Instant at) {
		var violations = new ArrayList<Violation>();
		String by = required("actor", actor, MAX_ACTOR_LENGTH, violations);
		throwIfAny(violations);
		if (status != IncidentStatus.OPEN) {
			throw new IncidentStateException("Incident " + id + " is already " + status.name().toLowerCase());
		}
		var next = new Incident(id, targetId, targetName, IncidentStatus.ACKNOWLEDGED, startedAt, openedAt, at, null,
				null, lastError);
		return recorded(next, TimelineEntryType.ACKNOWLEDGED, by, null, at);
	}

	/**
	 * Resolves by hand. The target may still be down, and no new incident opens until it recovers
	 * and fails again, so a reason is required and kept in the timeline.
	 */
	public Change resolve(String actor, String reason, Instant at) {
		var violations = new ArrayList<Violation>();
		String by = required("actor", actor, MAX_ACTOR_LENGTH, violations);
		String why = required("reason", reason, MAX_TEXT_LENGTH, violations);
		throwIfAny(violations);
		return resolved(Resolution.MANUAL, by, why, at);
	}

	/** Resolves because the target passed its recovery checks. */
	public Change recover(Instant at) {
		return resolved(Resolution.RECOVERED, SYSTEM_ACTOR, "Target recovered", at);
	}

	/** Resolves because the target was removed from monitoring. */
	public Change targetRemoved(Instant at) {
		return resolved(Resolution.TARGET_REMOVED, SYSTEM_ACTOR, "Target removed", at);
	}

	/** Adds a note. Allowed in every status, so findings can be added after resolution. */
	public Change addNote(String actor, String message, Instant at) {
		var violations = new ArrayList<Violation>();
		String by = required("actor", actor, MAX_ACTOR_LENGTH, violations);
		String text = required("message", message, MAX_TEXT_LENGTH, violations);
		throwIfAny(violations);
		return recorded(this, TimelineEntryType.NOTE, by, text, at);
	}

	public boolean isActive() {
		return status != IncidentStatus.RESOLVED;
	}

	/** Time from opening to acknowledgement (MTTA when averaged). */
	public Optional<Duration> timeToAcknowledge() {
		return Optional.ofNullable(acknowledgedAt).map(at -> Duration.between(openedAt, at));
	}

	/** Time from the start of the outage to resolution (MTTR when averaged). */
	public Optional<Duration> timeToResolve() {
		return Optional.ofNullable(resolvedAt).map(at -> Duration.between(startedAt, at));
	}

	private Change resolved(Resolution how, String actor, String message, Instant at) {
		if (!isActive()) {
			throw new IncidentStateException("Incident " + id + " is already resolved");
		}
		var next = new Incident(id, targetId, targetName, IncidentStatus.RESOLVED, startedAt, openedAt,
				acknowledgedAt, at, how, lastError);
		return recorded(next, TimelineEntryType.RESOLVED, actor, message, at);
	}

	private Change recorded(Incident next, TimelineEntryType type, String actor, String message, Instant at) {
		return new Change(next, new TimelineEntry(id, at, type, actor, message));
	}

	private static String required(String field, String value, int maxLength, List<Violation> violations) {
		String stripped = value == null ? "" : value.strip();
		if (stripped.isEmpty()) {
			violations.add(new Violation(field, "must not be blank"));
		}
		else if (stripped.length() > maxLength) {
			violations.add(new Violation(field, "must be at most " + maxLength + " characters"));
		}
		return stripped;
	}

	private static void throwIfAny(List<Violation> violations) {
		if (!violations.isEmpty()) {
			throw new ValidationException(violations);
		}
	}

	/**
	 * The incident after an action, plus the timeline entry that records it.
	 */
	public record Change(Incident incident, TimelineEntry entry) {

	}

}
