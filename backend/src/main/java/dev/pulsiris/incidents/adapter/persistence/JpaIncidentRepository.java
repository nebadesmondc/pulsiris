package dev.pulsiris.incidents.adapter.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import dev.pulsiris.incidents.application.IncidentRepository;
import dev.pulsiris.incidents.domain.Incident;
import dev.pulsiris.incidents.domain.IncidentStatus;

import org.springframework.stereotype.Repository;

@Repository
class JpaIncidentRepository implements IncidentRepository {

	private final IncidentJpaRepository jpa;

	private final TimelineEntryJpaRepository timeline;

	private final IncidentsEntityMapper mapper;

	JpaIncidentRepository(IncidentJpaRepository jpa, TimelineEntryJpaRepository timeline,
			IncidentsEntityMapper mapper) {
		this.jpa = jpa;
		this.timeline = timeline;
		this.mapper = mapper;
	}

	@Override
	public Optional<Incident> findById(UUID id) {
		return jpa.findById(id).map(mapper::toDomain);
	}

	@Override
	public Optional<Incident> findActiveByTargetId(UUID targetId) {
		return jpa.findByTargetIdAndStatusNot(targetId, IncidentStatus.RESOLVED).map(mapper::toDomain);
	}

	@Override
	public boolean existsByTargetIdAndStartedAt(UUID targetId, Instant startedAt) {
		return jpa.existsByTargetIdAndStartedAt(targetId, startedAt);
	}

	@Override
	public void save(Incident.Change change) {
		Incident incident = change.incident();
		// Update the managed entity when it exists, so its version is checked; otherwise insert.
		IncidentEntity entity = jpa.findById(incident.id()).map(existing -> {
			mapper.copyOnto(incident, existing);
			return existing;
		}).orElseGet(() -> mapper.toEntity(incident));
		jpa.save(entity);
		// Persisted after the incident, so Hibernate inserts the parent row first.
		timeline.save(mapper.toEntity(change.entry()));
	}

}
