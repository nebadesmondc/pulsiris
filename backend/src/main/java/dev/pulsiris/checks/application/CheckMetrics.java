package dev.pulsiris.checks.application;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

import dev.pulsiris.checks.domain.CheckOutcome;
import dev.pulsiris.checks.domain.CheckResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Component;

/**
 * Counts and times the checks this instance runs. Each instance records its own, so queries sum
 * them across instances.
 */
@Component
class CheckMetrics {

	private final Map<CheckOutcome, Timer> checks = new EnumMap<>(CheckOutcome.class);

	private final Timer delay;

	CheckMetrics(MeterRegistry registry) {
		// Registered up front, so every outcome reports 0 instead of being missing.
		for (CheckOutcome outcome : CheckOutcome.values()) {
			checks.put(outcome, Timer.builder("pulsiris.checks")
				.description("Probes of monitored targets, by outcome")
				.tag("outcome", outcome.name())
				.serviceLevelObjectives(millis(100), millis(250), millis(500), seconds(1), millis(2500), seconds(5),
						seconds(10), seconds(30))
				.register(registry));
		}
		this.delay = Timer.builder("pulsiris.check.delay")
			.description("How long after it was due a check started")
			.serviceLevelObjectives(seconds(1), seconds(5), seconds(10), seconds(30), seconds(60), seconds(300))
			.register(registry);
	}

	void recordCheck(CheckResult result) {
		checks.get(result.outcome()).record(result.latency());
	}

	void recordDelay(Duration late) {
		delay.record(late);
	}

	private static Duration millis(long millis) {
		return Duration.ofMillis(millis);
	}

	private static Duration seconds(long seconds) {
		return Duration.ofSeconds(seconds);
	}

}
