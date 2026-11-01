package dev.pulsiris.checks.adapter.metrics;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dev.pulsiris.checks.application.HealthQueries;
import dev.pulsiris.checks.application.TargetHealth;
import dev.pulsiris.checks.domain.HealthStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Current target health as gauges. Read from the database in the background, so a slow database
 * never slows down a scrape. Every instance reports the same values, so queries take the max.
 */
@Component
class TargetHealthMetrics {

	private static final Logger log = LoggerFactory.getLogger(TargetHealthMetrics.class);

	private final HealthQueries health;

	private final MultiGauge targetsByStatus;

	private final MultiGauge targetUp;

	TargetHealthMetrics(HealthQueries health, MeterRegistry registry) {
		this.health = health;
		this.targetsByStatus = MultiGauge.builder("pulsiris.targets")
			.description("Enabled targets by health status")
			.register(registry);
		this.targetUp = MultiGauge.builder("pulsiris.target.up")
			.description("1 if the target's last check succeeded, 0 if it failed")
			.register(registry);
	}

	@Scheduled(fixedDelay = 15, timeUnit = TimeUnit.SECONDS)
	void refresh() {
		List<TargetHealth> targets;
		try {
			targets = health.overview();
		}
		catch (RuntimeException ex) {
			// Keep the last values; the database problem shows up in the health and pool metrics.
			log.warn("Could not refresh target health metrics: {}", ex.getMessage());
			return;
		}

		Map<HealthStatus, Long> counts = new EnumMap<>(HealthStatus.class);
		for (HealthStatus status : HealthStatus.values()) {
			counts.put(status, 0L);
		}
		targets.forEach(target -> counts.merge(target.state().status(), 1L, Long::sum));
		targetsByStatus.register(counts.entrySet()
			.stream()
			.<MultiGauge.Row<?>>map(entry -> MultiGauge.Row.of(Tags.of("status", entry.getKey().name()),
					entry.getValue()))
			.toList(), true);

		// Overwriting drops the rows of removed or disabled targets.
		targetUp.register(targets.stream()
			.filter(target -> target.state().lastCheckedAt() != null)
			.<MultiGauge.Row<?>>map(target -> MultiGauge.Row.of(
					Tags.of("target_id", target.target().id().toString(), "target", target.target().name()),
					target.state().lastCheckSucceeded() ? 1 : 0))
			.toList(), true);
	}

}
