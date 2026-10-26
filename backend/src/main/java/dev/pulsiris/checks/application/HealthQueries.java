package dev.pulsiris.checks.application;

import java.util.List;
import java.util.UUID;

import dev.pulsiris.checks.domain.CheckResult;
import dev.pulsiris.checks.domain.HealthState;
import dev.pulsiris.registry.TargetLookup;
import dev.pulsiris.registry.TargetSnapshot;
import dev.pulsiris.shared.ValidationException;
import dev.pulsiris.shared.Violation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the checks module: current health and check history per target.
 */
@Service
@Transactional(readOnly = true)
public class HealthQueries {

	private final TargetLookup targets;

	private final HealthStateRepository states;

	private final CheckResultRepository results;

	HealthQueries(TargetLookup targets, HealthStateRepository states, CheckResultRepository results) {
		this.targets = targets;
		this.states = states;
		this.results = results;
	}

	/** A target that exists but has not been checked yet is reported as UNKNOWN. */
	public TargetHealth health(UUID targetId) {
		TargetSnapshot target = existing(targetId);
		HealthState state = states.findByTargetId(targetId).orElseGet(() -> HealthState.unknown(targetId));
		return new TargetHealth(target, state);
	}

	public Page<CheckResult> history(UUID targetId, CheckFilter filter, Pageable pageable) {
		existing(targetId);
		if (filter.from() != null && filter.to() != null && !filter.from().isBefore(filter.to())) {
			throw new ValidationException(List.of(new Violation("from", "must be before to")));
		}
		return results.findByTarget(targetId, filter, pageable);
	}

	private TargetSnapshot existing(UUID targetId) {
		return targets.findById(targetId).orElseThrow(() -> new UnknownTargetException(targetId));
	}

}
