package dev.pulsiris.registry.adapter.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

interface TargetJpaRepository extends JpaRepository<TargetEntity, UUID>, JpaSpecificationExecutor<TargetEntity> {

	// lower() on both sides matches the unique index on lower(name).
	@Query("select count(t) > 0 from TargetEntity t where lower(t.name) = lower(:name)")
	boolean existsByNameIgnoringCase(String name);

	@Query("select count(t) > 0 from TargetEntity t where lower(t.name) = lower(:name) and t.id <> :excludedId")
	boolean existsByNameIgnoringCaseExcluding(String name, UUID excludedId);

}
