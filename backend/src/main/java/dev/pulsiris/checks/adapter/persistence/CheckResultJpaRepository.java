package dev.pulsiris.checks.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface CheckResultJpaRepository
		extends JpaRepository<CheckResultEntity, Long>, JpaSpecificationExecutor<CheckResultEntity> {

}
