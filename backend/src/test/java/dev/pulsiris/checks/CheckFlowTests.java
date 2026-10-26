package dev.pulsiris.checks;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The whole check flow: registry → runner → prober → health state → events, against a fake
 * service and a clock the test moves forward.
 */
@IntegrationTest
@RecordApplicationEvents
@Import(TestClockConfiguration.class)
class CheckFlowTests {

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

	@Test
	void targetGoesDownAfterThreeFailuresAndRecoversAfterTwoSuccesses(ApplicationEvents events) {
		service.stubFor(get("/orders").willReturn(aResponse().withStatus(503)));
		UUID id = register("Orders", "/orders");

		runOnce();
		runOnce();
		assertThat(status(id)).isEqualTo("FAILING");
		assertThat(events.stream(TargetWentDown.class)).isEmpty();

		runOnce();
		assertThat(status(id)).isEqualTo("DOWN");
		assertThat(events.stream(TargetWentDown.class)).singleElement().satisfies(event -> {
			assertThat(event.targetId()).isEqualTo(id);
			assertThat(event.lastError()).isEqualTo("Unexpected status 503");
		});

		service.stubFor(get("/orders").willReturn(aResponse().withStatus(200)));
		runOnce();
		assertThat(status(id)).isEqualTo("DOWN");
		runOnce();
		assertThat(status(id)).isEqualTo("UP");
		assertThat(events.stream(TargetRecovered.class)).singleElement()
			.satisfies(event -> assertThat(event.targetId()).isEqualTo(id));
		assertThat(resultCount(id)).isEqualTo(5);
	}

	@Test
	void checksTargetsOnlyWhenTheirIntervalHasPassed() {
		service.stubFor(get("/billing").willReturn(aResponse().withStatus(200)));
		UUID id = register("Billing", "/billing");

		runner.runDueChecks();
		clock.advance(INTERVAL.minusSeconds(1));
		runner.runDueChecks();
		assertThat(resultCount(id)).isEqualTo(1);

		clock.advance(Duration.ofSeconds(1));
		runner.runDueChecks();
		assertThat(resultCount(id)).isEqualTo(2);
	}

	@Test
	void removingATargetDeletesItsHealthHistory() {
		service.stubFor(get("/legacy").willReturn(aResponse().withStatus(200)));
		UUID id = register("Legacy", "/legacy");
		runOnce();
		assertThat(resultCount(id)).isEqualTo(1);

		assertThat(mvc.delete().uri("/api/targets/{id}", id)).hasStatus2xxSuccessful();

		// The listener runs asynchronously after the registry's transaction commits.
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			assertThat(stateCount(id)).isZero();
			assertThat(resultCount(id)).isZero();
		});
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

	private void runOnce() {
		runner.runDueChecks();
		clock.advance(INTERVAL);
	}

	private String status(UUID id) {
		return jdbc.queryForObject("select status::text from checks.target_health_state where target_id = ?",
				String.class, id);
	}

	private int resultCount(UUID id) {
		return jdbc.queryForObject("select count(*) from checks.check_result where target_id = ?", Integer.class, id);
	}

	private int stateCount(UUID id) {
		return jdbc.queryForObject("select count(*) from checks.target_health_state where target_id = ?",
				Integer.class, id);
	}

}
