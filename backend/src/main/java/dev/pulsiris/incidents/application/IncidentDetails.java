package dev.pulsiris.incidents.application;

import java.util.List;

import dev.pulsiris.incidents.domain.Incident;
import dev.pulsiris.incidents.domain.TimelineEntry;

/**
 * An incident with its timeline, oldest entry first.
 */
public record IncidentDetails(Incident incident, List<TimelineEntry> timeline) {
}
