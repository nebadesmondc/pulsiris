package dev.pulsiris.incidents.application;

import java.time.Instant;
import java.util.UUID;

import dev.pulsiris.incidents.domain.Incident;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opens and resolves incidents as targets go down, recover or are removed. Events are delivered
 * at least once, so every method is safe to call again with the same input.
 */
@Service
public class IncidentService {

	private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

	private final IncidentRepository incidents;

	IncidentService(IncidentRepository incidents) {
		this.incidents = incidents;
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

	@Transactional
	public void resolveRecovered(UUID targetId, Instant outageStartedAt, Instant recoveredAt) {
		incidents.findActiveByTargetId(targetId)
			// A late recovery from an earlier outage must not resolve a newer incident.
			.filter(incident -> !incident.startedAt().isAfter(outageStartedAt))
			.ifPresent(incident -> {
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

}
