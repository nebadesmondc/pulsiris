package dev.pulsiris.incidents.adapter.metrics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.pulsiris.IntegrationTest;
import dev.pulsiris.MutableClock;
import dev.pulsiris.TestClockConfiguration;
import dev.pulsiris.checks.application.CheckRunner;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The open incidents gauge follows an outage from start to recovery. The database is shared with
 * other test classes, so their targets are disabled first and the gauge is compared with its value
 * before the outage.
 */
@IntegrationTest
@Import(TestClockConfiguration.class)
class IncidentMetricsTests {

	@RegisterExtension
	static WireMockExtension service = WireMockExtension.newInstance().build();

	private static final Duration INTERVAL = Duration.ofSeconds(10);

	@Autowired
	MockMvcTester mvc;

	@Autowired
	CheckRunner runner;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MeterRegistry registry;

	@Autowired
	IncidentMetrics metrics;

	@BeforeEach
	void disableOtherTargets() {
		jdbc.update("update registry.monitored_target set enabled = false");
	}

	@Test
	void countsAnIncidentWhileItIsOpen() {
		metrics.refresh();
		double before = open();

		service.stubFor(get("/search").willReturn(aResponse().withStatus(503)));
		UUID target = register("Search", "/search");
		for (int i = 0; i < 3; i++) {
			runOnce();
		}
		// The listener runs asynchronously after the check's transaction commits.
		await().atMost(Duration.ofSeconds(5)).until(() -> incidentStatus(target) != null);
		metrics.refresh();
		assertThat(open()).isEqualTo(before + 1);

		service.stubFor(get("/search").willReturn(aResponse().withStatus(200)));
		runOnce();
		runOnce();
		await().atMost(Duration.ofSeconds(5)).until(() -> "RESOLVED".equals(incidentStatus(target)));
		metrics.refresh();
		assertThat(open()).isEqualTo(before);
	}

	private double open() {
		return registry.get("pulsiris.incidents.open").tag("status", "OPEN").gauge().value();
	}

	private String incidentStatus(UUID target) {
		return jdbc.query("select status::text from incidents.incident where target_id = ?",
				rs -> rs.next() ? rs.getString(1) : null, target);
	}

	private void runOnce() {
		runner.runDueChecks();
		clock.advance(INTERVAL);
	}

	private UUID register(String name, String path) {
		String json = """
				{ "name": "%s %s", "endpointUrl": "%s%s", "intervalSeconds": %d, "timeoutMs": 1000 }
				""".formatted(name, UUID.randomUUID().toString().substring(0, 8), service.baseUrl(), path,
				INTERVAL.toSeconds());
		String location = mvc.post()
			.uri("/api/targets")
			.contentType(MediaType.APPLICATION_JSON)
			.content(json)
			.exchange()
			.getResponse()
			.getHeader("Location");
		return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
	}

}
