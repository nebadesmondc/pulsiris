package dev.pulsiris.incidents.adapter.metrics;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import dev.pulsiris.incidents.application.IncidentQueries;
import dev.pulsiris.incidents.domain.IncidentStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Unresolved incidents as a gauge. Read from the database in the background, so a slow database
 * never slows down a scrape. Every instance reports the same values, so queries take the max.
 */
@Component
class IncidentMetrics {

	private static final Logger log = LoggerFactory.getLogger(IncidentMetrics.class);

	private final IncidentQueries incidents;

	private final MultiGauge open;

	IncidentMetrics(IncidentQueries incidents, MeterRegistry registry) {
		this.incidents = incidents;
		this.open = MultiGauge.builder("pulsiris.incidents.open")
			.description("Incidents not resolved yet, by status")
			.register(registry);
	}

	@Scheduled(fixedDelay = 15, timeUnit = TimeUnit.SECONDS)
	void refresh() {
		Map<IncidentStatus, Long> counts;
		try {
			counts = incidents.countActive();
		}
		catch (RuntimeException ex) {
			// Keep the last values; the database problem shows up in the health and pool metrics.
			log.warn("Could not refresh incident metrics: {}", ex.getMessage());
			return;
		}
		open.register(counts.entrySet()
			.stream()
			.<MultiGauge.Row<?>>map(entry -> MultiGauge.Row.of(Tags.of("status", entry.getKey().name()),
					entry.getValue()))
			.toList(), true);
	}

}
