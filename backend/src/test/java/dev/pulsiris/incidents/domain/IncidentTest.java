package dev.pulsiris.incidents.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import dev.pulsiris.shared.ValidationException;
import dev.pulsiris.shared.Violation;
import org.junit.jupiter.api.Test;

class IncidentTest {

	private static final UUID TARGET = UUID.randomUUID();

	private static final Instant STARTED = Instant.parse("2026-10-27T09:00:00Z");

	private static final Instant DETECTED = STARTED.plusSeconds(120);

	private final Incident.Change opened = Incident.open(TARGET, "Orders", STARTED, DETECTED, "Unexpected status 503");

	private final Incident incident = opened.incident();

	@Test
	void opensWithTheOutageStartAndASystemEntry() {
		assertThat(incident.status()).isEqualTo(IncidentStatus.OPEN);
		assertThat(incident.startedAt()).isEqualTo(STARTED);
		assertThat(incident.openedAt()).isEqualTo(DETECTED);
		assertThat(incident.isActive()).isTrue();
		assertThat(opened.entry()).isEqualTo(new TimelineEntry(incident.id(), DETECTED, TimelineEntryType.OPENED,
				Incident.SYSTEM_ACTOR, "Unexpected status 503"));
	}

	@Test
	void acknowledgingRecordsWhoAndWhen() {
		Incident.Change change = incident.acknowledge(" user-x ", at(5));

		assertThat(change.incident().status()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
		assertThat(change.incident().timeToAcknowledge()).contains(Duration.ofMinutes(5));
		assertThat(change.entry().type()).isEqualTo(TimelineEntryType.ACKNOWLEDGED);
		assertThat(change.entry().actor()).isEqualTo("user-x");
	}

	@Test
	void canBeAcknowledgedOnlyOnce() {
		Incident acknowledged = incident.acknowledge("user-x", at(5)).incident();

		assertThatExceptionOfType(IncidentStateException.class)
			.isThrownBy(() -> acknowledged.acknowledge("user-y", at(6)))
			.withMessageEndingWith("is already acknowledged");
	}

	@Test
	void recoveryResolvesAnAcknowledgedIncidentAndKeepsTheAcknowledgement() {
		Incident acknowledged = incident.acknowledge("user-x", at(5)).incident();

		Incident.Change change = acknowledged.recover(at(30));

		assertThat(change.incident().status()).isEqualTo(IncidentStatus.RESOLVED);
		assertThat(change.incident().resolution()).isEqualTo(Resolution.RECOVERED);
		assertThat(change.incident().acknowledgedAt()).isEqualTo(at(5));
		assertThat(change.incident().timeToResolve()).contains(Duration.ofMinutes(32));
		assertThat(change.entry().actor()).isEqualTo(Incident.SYSTEM_ACTOR);
	}

	@Test
	void manualResolveKeepsTheReason() {
		Incident.Change change = incident.resolve("user-x", "Planned maintenance", at(10));

		assertThat(change.incident().resolution()).isEqualTo(Resolution.MANUAL);
		assertThat(change.entry().type()).isEqualTo(TimelineEntryType.RESOLVED);
		assertThat(change.entry().message()).isEqualTo("Planned maintenance");
	}

	@Test
	void manualResolveNeedsAnActorAndAReason() {
		assertThatExceptionOfType(ValidationException.class).isThrownBy(() -> incident.resolve(" ", null, at(10)))
			.satisfies(ex -> assertThat(ex.violations()).containsExactly(
					new Violation("actor", "must not be blank"), new Violation("reason", "must not be blank")));
	}

	@Test
	void resolvedIncidentsNeverChangeStatusAgain() {
		Incident resolved = incident.recover(at(10)).incident();

		assertThatExceptionOfType(IncidentStateException.class).isThrownBy(() -> resolved.acknowledge("user-x", at(11)));
		assertThatExceptionOfType(IncidentStateException.class)
			.isThrownBy(() -> resolved.resolve("user-x", "Done", at(11)));
		assertThatExceptionOfType(IncidentStateException.class).isThrownBy(() -> resolved.recover(at(11)));
		assertThatExceptionOfType(IncidentStateException.class).isThrownBy(() -> resolved.targetRemoved(at(11)));
	}

	@Test
	void removingTheTargetResolvesTheIncident() {
		Incident.Change change = incident.targetRemoved(at(3));

		assertThat(change.incident().resolution()).isEqualTo(Resolution.TARGET_REMOVED);
	}

	@Test
	void notesAreAllowedAfterResolutionAndLeaveTheIncidentUnchanged() {
		Incident resolved = incident.recover(at(10)).incident();

		Incident.Change change = resolved.addNote("user-x", "Root cause: expired certificate", at(60));

		assertThat(change.incident()).isEqualTo(resolved);
		assertThat(change.entry().type()).isEqualTo(TimelineEntryType.NOTE);
	}

	@Test
	void notesHaveALengthLimit() {
		assertThatExceptionOfType(ValidationException.class)
			.isThrownBy(() -> incident.addNote("user-x", "x".repeat(Incident.MAX_TEXT_LENGTH + 1), at(1)))
			.satisfies(ex -> assertThat(ex.violations()).extracting(Violation::field).containsExactly("message"));
	}

	@Test
	void notesUnobservedTimeAsASystemEntry() {
		Incident.Change change = incident.noteUnobserved(Duration.ofMinutes(150), at(160));

		assertThat(change.incident()).isEqualTo(incident);
		assertThat(change.entry().actor()).isEqualTo(Incident.SYSTEM_ACTOR);
		assertThat(change.entry().message())
			.isEqualTo("Not monitored for 2h 30m during this outage; the recovery time is approximate");
		assertThat(incident.noteUnobserved(Duration.ofHours(51).plusMinutes(5), at(1)).entry().message())
			.startsWith("Not monitored for 2d 3h during");
	}

	private static Instant at(int minutesAfterDetection) {
		return DETECTED.plus(Duration.ofMinutes(minutesAfterDetection));
	}

}
