package dev.pulsiris.checks;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a DOWN target passes its configured number of checks in a row.
 *
 * @param failingSince when the outage started
 * @param unobserved how long the target went unchecked during the outage, e.g. while Pulsiris was
 * offline; when not zero, the outage's real end is uncertain
 */
public record TargetRecovered(UUID targetId, String targetName, Instant failingSince, Instant recoveredAt,
		Duration unobserved) {
}
