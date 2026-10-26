package dev.pulsiris.checks.adapter.persistence;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import dev.pulsiris.checks.application.HealthStateRepository;
import dev.pulsiris.checks.domain.HealthState;

import org.springframework.stereotype.Repository;

@Repository
class JpaHealthStateRepository implements HealthStateRepository {

	private final HealthStateJpaRepository jpa;

	private final ChecksEntityMapper mapper;

	JpaHealthStateRepository(HealthStateJpaRepository jpa, ChecksEntityMapper mapper) {
		this.jpa = jpa;
		this.mapper = mapper;
	}

	@Override
	public Optional<HealthState> findByTargetId(UUID targetId) {
		return jpa.findById(targetId).map(mapper::toDomain);
	}

	@Override
	public Map<UUID, HealthState> findByTargetIds(Collection<UUID> targetIds) {
		return jpa.findAllById(targetIds)
			.stream()
			.map(mapper::toDomain)
			.collect(Collectors.toMap(HealthState::targetId, Function.identity()));
	}

	@Override
	public void save(HealthState state) {
		// Update the managed entity when it exists, so its version is kept; otherwise insert.
		HealthStateEntity entity = jpa.findById(state.targetId()).map(existing -> {
			mapper.copyOnto(state, existing);
			return existing;
		}).orElseGet(() -> mapper.toEntity(state));
		jpa.save(entity);
	}

	@Override
	public void deleteByTargetId(UUID targetId) {
		jpa.deleteById(targetId);
	}

}
