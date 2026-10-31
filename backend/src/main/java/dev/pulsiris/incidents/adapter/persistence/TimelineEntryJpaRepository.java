package dev.pulsiris.incidents.adapter.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TimelineEntryJpaRepository extends JpaRepository<TimelineEntryEntity, Long> {

	List<TimelineEntryEntity> findByIncidentIdOrderByOccurredAtAscIdAsc(UUID incidentId);

}
