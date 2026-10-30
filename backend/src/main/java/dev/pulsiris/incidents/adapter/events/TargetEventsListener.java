package dev.pulsiris.incidents.adapter.events;

import dev.pulsiris.checks.TargetRecovered;
import dev.pulsiris.checks.TargetWentDown;
import dev.pulsiris.incidents.application.IncidentService;
import dev.pulsiris.registry.TargetRemoved;

import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Turns health and registry events into incident changes. Each listener runs asynchronously after
 * the publisher's transaction commits; the event is persisted until it succeeds.
 */
@Component
class TargetEventsListener {

	private final IncidentService incidents;

	TargetEventsListener(IncidentService incidents) {
		this.incidents = incidents;
	}

	@ApplicationModuleListener
	void on(TargetWentDown event) {
		incidents.openForOutage(event.targetId(), event.targetName(), event.failingSince(), event.detectedAt(),
				event.lastError());
	}

	@ApplicationModuleListener
	void on(TargetRecovered event) {
		incidents.resolveRecovered(event.targetId(), event.failingSince(), event.recoveredAt());
	}

	@ApplicationModuleListener
	void on(TargetRemoved event) {
		incidents.resolveRemovedTarget(event.targetId(), event.removedAt());
	}

}
