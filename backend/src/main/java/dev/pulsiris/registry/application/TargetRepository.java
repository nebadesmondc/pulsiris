package dev.pulsiris.registry.application;

import java.util.Optional;
import java.util.UUID;

import dev.pulsiris.registry.domain.Target;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TargetRepository {

	Target save(Target target);

	Optional<Target> findById(UUID id);

	Page<Target> findAll(TargetFilter filter, Pageable pageable);

	boolean existsByName(String name);

	boolean existsByNameExcluding(String name, UUID excludedId);

	void delete(UUID id);

}
