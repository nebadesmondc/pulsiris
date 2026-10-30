package dev.pulsiris.incidents.adapter.web;

import java.time.Instant;
import java.util.List;

import dev.pulsiris.incidents.application.IncidentDetails;
import dev.pulsiris.incidents.domain.TimelineEntry;
import dev.pulsiris.incidents.domain.TimelineEntryType;

record IncidentDetailsResponse(IncidentResponse incident, List<TimelineEntryResponse> timeline) {

	static IncidentDetailsResponse from(IncidentDetails details) {
		return new IncidentDetailsResponse(IncidentResponse.from(details.incident()),
				details.timeline().stream().map(TimelineEntryResponse::from).toList());
	}

	record TimelineEntryResponse(Instant occurredAt, TimelineEntryType type, String actor, String message) {

		static TimelineEntryResponse from(TimelineEntry entry) {
			return new TimelineEntryResponse(entry.occurredAt(), entry.type(), entry.actor(), entry.message());
		}

	}

}
