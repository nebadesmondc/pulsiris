package dev.pulsiris.checks.application;

import dev.pulsiris.checks.domain.HealthState;
import dev.pulsiris.registry.TargetSnapshot;

/**
 * A target's current health together with the registry details needed to present it.
 */
public record TargetHealth(TargetSnapshot target, HealthState state) {
}
