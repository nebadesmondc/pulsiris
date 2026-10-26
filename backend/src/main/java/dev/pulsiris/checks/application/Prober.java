package dev.pulsiris.checks.application;

import dev.pulsiris.checks.domain.CheckResult;
import dev.pulsiris.checks.domain.ProbeTarget;

/**
 * Outbound port: checks a target once and reports what happened. Never throws for problems
 * with the target itself; those become the result's outcome.
 */
public interface Prober {

	CheckResult probe(ProbeTarget target);

}
