package dev.pulsiris.incidents.adapter.web;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import dev.pulsiris.incidents.domain.Incident;
import dev.pulsiris.incidents.domain.IncidentStatus;
import dev.pulsiris.incidents.domain.Resolution;
import io.swagger.v3.oas.annotations.media.Schema;

record IncidentResponse(
		UUID id,
		UUID targetId,
		@Schema(description = "Target name when the incident opened") String targetName,
		IncidentStatus status,
		@Schema(description = "When the outage started: the target's first failed check") Instant startedAt,
		@Schema(description = "When the outage was confirmed") Instant openedAt,
		Instant acknowledgedAt,
		Instant resolvedAt,
		Resolution resolution,
		@Schema(description = "What the confirming check reported") String lastError,
		@Schema(description = "Seconds from opening to acknowledgement") Long timeToAcknowledgeSeconds,
		@Schema(description = "Seconds from the start of the outage to resolution") Long timeToResolveSeconds) {

	static IncidentResponse from(Incident incident) {
		return new IncidentResponse(incident.id(), incident.targetId(), incident.targetName(), incident.status(),
				incident.startedAt(), incident.openedAt(), incident.acknowledgedAt(), incident.resolvedAt(),
				incident.resolution(), incident.lastError(),
				incident.timeToAcknowledge().map(Duration::toSeconds).orElse(null),
				incident.timeToResolve().map(Duration::toSeconds).orElse(null));
	}

}
