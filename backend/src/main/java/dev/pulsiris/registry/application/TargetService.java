package dev.pulsiris.registry.application;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import dev.pulsiris.registry.TargetRemoved;
import dev.pulsiris.registry.domain.DuplicateTargetNameException;
import dev.pulsiris.registry.domain.Target;
import dev.pulsiris.registry.domain.TargetDraft;
import dev.pulsiris.registry.domain.TargetNotFoundException;
import dev.pulsiris.registry.domain.TargetPolicy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TargetService {

	private final TargetRepository repository;

	private final TargetPolicy policy;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	TargetService(TargetRepository repository, TargetPolicy policy, ApplicationEventPublisher events, Clock clock) {
		this.repository = repository;
		this.policy = policy;
		this.events = events;
		this.clock = clock;
	}

	public Target register(TargetDraft draft) {
		Target target = Target.register(draft, policy, now());
		if (repository.existsByName(target.name())) {
			throw new DuplicateTargetNameException(target.name());
		}
		return repository.save(target);
	}

	@Transactional(readOnly = true)
	public Target get(UUID id) {
		return repository.findById(id).orElseThrow(() -> new TargetNotFoundException(id));
	}

	@Transactional(readOnly = true)
	public Page<Target> list(TargetFilter filter, Pageable pageable) {
		return repository.findAll(filter, pageable);
	}

	public Target update(UUID id, TargetDraft draft) {
		Target updated = get(id).update(draft, policy, now());
		if (repository.existsByNameExcluding(updated.name(), id)) {
			throw new DuplicateTargetNameException(updated.name());
		}
		return repository.save(updated);
	}

	public void remove(UUID id) {
		Target target = get(id);
		repository.delete(id);
		// Published inside the transaction: listeners in other modules only see it if the delete commits.
		events.publishEvent(new TargetRemoved(target.id(), target.name(), now()));
	}

	// PostgreSQL stores microseconds; truncating keeps returned values equal to stored ones.
	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.MICROS);
	}

}
