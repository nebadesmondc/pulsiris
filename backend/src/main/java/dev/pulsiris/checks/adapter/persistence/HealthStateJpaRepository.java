package dev.pulsiris.checks.adapter.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface HealthStateJpaRepository extends JpaRepository<HealthStateEntity, UUID> {

}
