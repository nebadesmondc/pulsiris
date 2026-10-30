package dev.pulsiris.incidents.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import dev.pulsiris.incidents.domain.Incident;

public interface IncidentRepository {

	Optional<Incident> findById(UUID id);

	/** The target's incident that is not resolved yet; there is at most one. */
	Optional<Incident> findActiveByTargetId(UUID targetId);

	boolean existsByTargetIdAndStartedAt(UUID targetId, Instant startedAt);

	/** Stores the incident's new state together with the timeline entry that records it. */
	void save(Incident.Change change);

}
