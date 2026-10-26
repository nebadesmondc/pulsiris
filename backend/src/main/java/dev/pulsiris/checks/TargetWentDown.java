package dev.pulsiris.checks;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a target fails its configured number of checks in a row.
 *
 * @param failingSince when the first failure of the outage happened
 * @param detectedAt when the check that confirmed the outage ran
 * @param lastError what the confirming check reported
 */
public record TargetWentDown(UUID targetId, String targetName, Instant failingSince, Instant detectedAt,
		String lastError) {
}
