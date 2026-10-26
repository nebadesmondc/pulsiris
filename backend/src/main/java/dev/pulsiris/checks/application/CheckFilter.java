package dev.pulsiris.checks.application;

import java.time.Instant;

import dev.pulsiris.checks.domain.CheckOutcome;

/**
 * Optional criteria for a target's check history. A {@code null} field means "don't filter on it".
 *
 * @param from inclusive lower bound on the check time
 * @param to exclusive upper bound on the check time
 */
public record CheckFilter(CheckOutcome outcome, Instant from, Instant to) {
}
