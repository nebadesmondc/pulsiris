package dev.pulsiris.registry;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a target is removed, so other modules can clean up their own data.
 */
public record TargetRemoved(UUID targetId, String name, Instant removedAt) {
}
