package dev.pulsiris.incidents.application;

import java.time.Instant;
import java.util.UUID;

import dev.pulsiris.incidents.domain.IncidentStatus;

/**
 * Optional criteria for listing incidents. A {@code null} field means "don't filter on it".
 *
 * @param from inclusive lower bound on the opening time
 * @param to exclusive upper bound on the opening time
 */
public record IncidentFilter(IncidentStatus status, UUID targetId, Instant from, Instant to) {
}
