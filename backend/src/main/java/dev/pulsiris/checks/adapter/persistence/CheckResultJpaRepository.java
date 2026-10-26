package dev.pulsiris.checks.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface CheckResultJpaRepository extends JpaRepository<CheckResultEntity, Long> {

}
