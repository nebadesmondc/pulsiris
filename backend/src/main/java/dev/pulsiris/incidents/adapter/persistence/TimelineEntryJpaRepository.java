package dev.pulsiris.incidents.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface TimelineEntryJpaRepository extends JpaRepository<TimelineEntryEntity, Long> {

}
