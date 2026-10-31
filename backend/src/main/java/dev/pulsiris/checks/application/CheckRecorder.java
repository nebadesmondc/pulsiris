package dev.pulsiris.checks.application;

import dev.pulsiris.checks.TargetRecovered;
import dev.pulsiris.checks.TargetWentDown;
import dev.pulsiris.checks.config.ChecksProperties;
import dev.pulsiris.checks.domain.CheckResult;
import dev.pulsiris.checks.domain.HealthState;
import dev.pulsiris.checks.domain.Thresholds;
import dev.pulsiris.registry.TargetSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies one check result: updates the health state, stores the result and announces status
 * changes, all in one transaction.
 */
@Service
public class CheckRecorder {

	private static final Logger log = LoggerFactory.getLogger(CheckRecorder.class);

	private final HealthStateRepository states;

	private final CheckResultRepository results;

	private final ApplicationEventPublisher events;

	private final ChecksProperties properties;

	CheckRecorder(HealthStateRepository states, CheckResultRepository results, ApplicationEventPublisher events,
			ChecksProperties properties) {
		this.states = states;
		this.results = results;
		this.events = events;
		this.properties = properties;
	}

	@Transactional
	public HealthState record(TargetSnapshot target, CheckResult result) {
		HealthState current = states.findByTargetId(target.id()).orElseGet(() -> HealthState.unknown(target.id()));
		HealthState.Evaluation evaluation = current.apply(result,
				new Thresholds(target.failureThreshold(), target.recoveryThreshold()),
				target.interval().multipliedBy(properties.staleAfterIntervals()));
		HealthState next = evaluation.state();

		// State first: check_result has a foreign key to it.
		states.save(next);
		results.save(result);

		evaluation.statusChange().ifPresent(change -> {
			switch (change) {
				case WENT_DOWN -> {
					log.warn("Target '{}' is DOWN: {}", target.name(), result.errorMessage());
					events.publishEvent(new TargetWentDown(target.id(), target.name(), next.failingSince(),
							result.checkedAt(), result.errorMessage()));
				}
				case RECOVERED -> {
					log.info("Target '{}' recovered", target.name());
					events.publishEvent(new TargetRecovered(target.id(), target.name(), current.failingSince(),
							result.checkedAt(), evaluation.unobservedDuringOutage()));
				}
			}
		});
		return next;
	}

}
