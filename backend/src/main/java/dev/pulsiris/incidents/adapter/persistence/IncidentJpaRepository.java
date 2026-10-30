package dev.pulsiris.incidents.adapter.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import dev.pulsiris.incidents.domain.IncidentStatus;

import org.springframework.data.jpa.repository.JpaRepository;

interface IncidentJpaRepository extends JpaRepository<IncidentEntity, UUID> {

	Optional<IncidentEntity> findByTargetIdAndStatusNot(UUID targetId, IncidentStatus status);

	boolean existsByTargetIdAndStartedAt(UUID targetId, Instant startedAt);

}
