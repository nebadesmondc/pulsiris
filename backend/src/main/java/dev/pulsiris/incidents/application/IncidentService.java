package dev.pulsiris.incidents.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Function;

import dev.pulsiris.incidents.domain.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opens and resolves incidents as targets go down, recover or are removed, and applies the
 * actions people take. Events are delivered at least once, so the event methods are safe to call
 * again with the same input.
 */
@Service
public class IncidentService {

	private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

	private final IncidentRepository incidents;

	private final Clock clock;

	IncidentService(IncidentRepository incidents, Clock clock) {
		this.incidents = incidents;
		this.clock = clock;
	}

	@Transactional
	public void openForOutage(UUID targetId, String targetName, Instant startedAt, Instant detectedAt,
			String lastError) {
		if (incidents.existsByTargetIdAndStartedAt(targetId, startedAt)) {
			// This outage already has an incident: the event was delivered again.
			return;
		}
		var active = incidents.findActiveByTargetId(targetId);
		if (active.isPresent()) {
			log.warn("Target '{}' went down while incident {} is still active; not opening another", targetName,
					active.get().id());
			return;
		}
		Incident.Change change = Incident.open(targetId, targetName, startedAt, detectedAt, lastError);
		incidents.save(change);
		log.warn("Opened incident {} for target '{}'", change.incident().id(), targetName);
	}

	/**
	 * @param unobserved how long the outage went unchecked; a note in the timeline when not zero
	 */
	@Transactional
	public void resolveRecovered(UUID targetId, Instant outageStartedAt, Instant recoveredAt, Duration unobserved) {
		incidents.findActiveByTargetId(targetId)
			// A late recovery from an earlier outage must not resolve a newer incident.
			.filter(incident -> !incident.startedAt().isAfter(outageStartedAt))
			.ifPresent(incident -> {
				if (unobserved != null && unobserved.isPositive()) {
					incidents.save(incident.noteUnobserved(unobserved, recoveredAt));
				}
				incidents.save(incident.recover(recoveredAt));
				log.info("Resolved incident {}: target '{}' recovered", incident.id(), incident.targetName());
			});
	}

	@Transactional
	public void resolveRemovedTarget(UUID targetId, Instant removedAt) {
		incidents.findActiveByTargetId(targetId).ifPresent(incident -> {
			incidents.save(incident.targetRemoved(removedAt));
			log.info("Resolved incident {}: target '{}' was removed", incident.id(), incident.targetName());
		});
	}

	@Transactional
	public void acknowledge(UUID id, String actor) {
		apply(id, incident -> incident.acknowledge(actor, now()));
	}

	@Transactional
	public void resolve(UUID id, String actor, String reason) {
		Incident resolved = apply(id, incident -> incident.resolve(actor, reason, now()));
		// The target may still be down; no new incident opens until it recovers and fails again.
		log.warn("Incident {} for target '{}' resolved manually by {}", id, resolved.targetName(), actor);
	}

	@Transactional
	public void addNote(UUID id, String actor, String message) {
		apply(id, incident -> incident.addNote(actor, message, now()));
	}

	private Incident apply(UUID id, Function<Incident, Incident.Change> action) {
		Incident incident = incidents.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
		Incident.Change change = action.apply(incident);
		incidents.save(change);
		return change.incident();
	}

	private Instant now() {
		// PostgreSQL stores microseconds; truncating keeps the stored and returned values equal.
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
