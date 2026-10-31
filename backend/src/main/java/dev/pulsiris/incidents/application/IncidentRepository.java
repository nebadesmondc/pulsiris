package dev.pulsiris.incidents.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.pulsiris.incidents.domain.Incident;
import dev.pulsiris.incidents.domain.TimelineEntry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IncidentRepository {

	Optional<Incident> findById(UUID id);

	Page<Incident> findAll(IncidentFilter filter, Pageable pageable);

	/** The target's incident that is not resolved yet; there is at most one. */
	Optional<Incident> findActiveByTargetId(UUID targetId);

	boolean existsByTargetIdAndStartedAt(UUID targetId, Instant startedAt);

	/** Oldest entry first. */
	List<TimelineEntry> findTimeline(UUID incidentId);

	/** Stores the incident's new state together with the timeline entry that records it. */
	void save(Incident.Change change);

}
