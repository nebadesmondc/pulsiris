package dev.pulsiris.checks.adapter.metrics;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.pulsiris.IntegrationTest;
import dev.pulsiris.MutableClock;
import dev.pulsiris.TestClockConfiguration;
import dev.pulsiris.checks.application.CheckRunner;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Check timers recorded by the runner, and the health gauges read back from the database. The
 * database is shared with other test classes, so their targets are disabled first and the timers
 * are compared with their values before the test.
 */
@IntegrationTest
@Import(TestClockConfiguration.class)
class ChecksMetricsTests {

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
	MeterRegistry registry;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	TargetHealthMetrics healthMetrics;

	@BeforeEach
	void disableOtherTargets() {
		jdbc.update("update registry.monitored_target set enabled = false");
	}

	@Test
	void timesChecksByOutcomeAndRecordsHowLateTheyStarted() {
		service.stubFor(get("/slow-start").willReturn(aResponse().withStatus(503)));
		register("Slow start", "/slow-start");
		long failures = checks("FAILURE").count();
		long delays = delay().count();
		double delayed = delay().totalTime(TimeUnit.SECONDS);

		runner.runDueChecks();
		clock.advance(INTERVAL.plusSeconds(3));
		runner.runDueChecks();

		assertThat(checks("FAILURE").count() - failures).isEqualTo(2);
		// The first check had no due time; the second started 3 s after it was due.
		assertThat(delay().count() - delays).isEqualTo(1);
		assertThat(delay().totalTime(TimeUnit.SECONDS) - delayed).isEqualTo(3);
		// Registered up front, so outcomes that never happened still report 0.
		assertThat(registry.find("pulsiris.checks").tag("outcome", "TIMEOUT").timer()).isNotNull();
	}

	@Test
	void reportsEachTargetsLastResultAndCountsTargetsByStatus() {
		service.stubFor(get("/healthy").willReturn(aResponse().withStatus(200)));
		service.stubFor(get("/broken").willReturn(aResponse().withStatus(503)));
		UUID healthy = register("Healthy", "/healthy");
		UUID broken = register("Broken", "/broken");
		for (int i = 0; i < 3; i++) {
			runner.runDueChecks();
			clock.advance(INTERVAL);
		}
		UUID unchecked = register("Unchecked", "/healthy");

		healthMetrics.refresh();

		assertThat(targetUp(healthy).value()).isEqualTo(1);
		assertThat(targetUp(broken).value()).isEqualTo(0);
		assertThat(targetUp(unchecked)).isNull();
		assertThat(targets("UP")).isEqualTo(1);
		assertThat(targets("DOWN")).isEqualTo(1);
		assertThat(targets("UNKNOWN")).isEqualTo(1);
		assertThat(targets("FAILING")).isZero();
	}

	@Test
	void dropsRemovedTargets() {
		service.stubFor(get("/retired").willReturn(aResponse().withStatus(200)));
		UUID retired = register("Retired", "/retired");
		runner.runDueChecks();
		healthMetrics.refresh();
		assertThat(targetUp(retired)).isNotNull();

		assertThat(mvc.delete().uri("/api/targets/{id}", retired)).hasStatus2xxSuccessful();
		healthMetrics.refresh();

		assertThat(targetUp(retired)).isNull();
	}

	private Timer checks(String outcome) {
		return registry.get("pulsiris.checks").tag("outcome", outcome).timer();
	}

	private Timer delay() {
		return registry.get("pulsiris.check.delay").timer();
	}

	private Gauge targetUp(UUID id) {
		return registry.find("pulsiris.target.up").tag("target_id", id.toString()).gauge();
	}

	private double targets(String status) {
		return registry.get("pulsiris.targets").tag("status", status).gauge().value();
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
