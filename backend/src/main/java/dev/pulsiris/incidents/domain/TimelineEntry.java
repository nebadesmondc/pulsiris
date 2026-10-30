package dev.pulsiris.incidents.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * One action in an incident's history. Entries are only ever appended.
 *
 * @param actor who acted: a person, or {@link Incident#SYSTEM_ACTOR}
 * @param message the error, reason or note, if any
 */
public record TimelineEntry(UUID incidentId, Instant occurredAt, TimelineEntryType type, String actor,
		String message) {

	public TimelineEntry {
		Objects.requireNonNull(incidentId, "incidentId");
		Objects.requireNonNull(occurredAt, "occurredAt");
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(actor, "actor");
	}

}
