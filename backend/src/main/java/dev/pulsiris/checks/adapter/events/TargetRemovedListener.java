package dev.pulsiris.checks.adapter.events;

import dev.pulsiris.checks.application.HealthStateRepository;
import dev.pulsiris.registry.TargetRemoved;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Deletes a removed target's health state and, through the foreign key, its check results.
 * Runs asynchronously after the registry's transaction commits; the event is persisted until
 * this listener succeeds.
 */
@Component
class TargetRemovedListener {

	private final HealthStateRepository states;

	TargetRemovedListener(HealthStateRepository states) {
		this.states = states;
	}

	@ApplicationModuleListener
	void on(TargetRemoved event) {
		states.deleteByTargetId(event.targetId());
	}

}
