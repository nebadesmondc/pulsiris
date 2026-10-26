package dev.pulsiris.checks;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a DOWN target passes its configured number of checks in a row.
 *
 * @param failingSince when the outage started
 */
public record TargetRecovered(UUID targetId, String targetName, Instant failingSince, Instant recoveredAt) {
}
