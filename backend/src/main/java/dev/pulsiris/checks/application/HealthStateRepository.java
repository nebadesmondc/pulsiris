package dev.pulsiris.checks.application;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import dev.pulsiris.checks.domain.HealthState;

public interface HealthStateRepository {

	Optional<HealthState> findByTargetId(UUID targetId);

	Map<UUID, HealthState> findByTargetIds(Collection<UUID> targetIds);

	void save(HealthState state);

	/** Also removes the target's check results. */
	void deleteByTargetId(UUID targetId);

}
