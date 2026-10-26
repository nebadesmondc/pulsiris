package dev.pulsiris.registry.application;

import java.util.List;

import dev.pulsiris.registry.TargetLookup;
import dev.pulsiris.registry.TargetSnapshot;
import dev.pulsiris.registry.domain.Target;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class RegistryTargetLookup implements TargetLookup {

	private final TargetRepository repository;

	RegistryTargetLookup(TargetRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<TargetSnapshot> findEnabled() {
		return repository.findAllEnabled().stream().map(RegistryTargetLookup::snapshot).toList();
	}

	private static TargetSnapshot snapshot(Target target) {
		return new TargetSnapshot(target.id(), target.name(), target.endpointUrl(), target.httpMethod().name(),
				target.expectedStatus(), target.interval(), target.timeout(), target.failureThreshold(),
				target.recoveryThreshold());
	}

}
