package dev.pulsiris.incidents;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.pulsiris.IntegrationTest;
import dev.pulsiris.MutableClock;
import dev.pulsiris.TestClockConfiguration;
import dev.pulsiris.checks.application.CheckRunner;
import dev.pulsiris.incidents.application.IncidentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Incidents opened and resolved by real health events, plus the redelivery and ordering cases
 * that the event registry can produce.
 */
@IntegrationTest
@Import(TestClockConfiguration.class)
class IncidentFlowTests {

	@RegisterExtension
	static WireMockExtension service = WireMockExtension.newInstance().build();

	private static final Duration INTERVAL = Duration.ofSeconds(10);

	private static final Instant T0 = Instant.parse("2026-10-27T09:00:00Z");

	@Autowired
	MockMvcTester mvc;

	@Autowired
	CheckRunner runner;

	@Autowired
	IncidentService incidents;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void outageOpensAnIncidentAndRecoveryResolvesIt() {
		service.stubFor(get("/payments").willReturn(aResponse().withStatus(503)));
		UUID target = register("Payments", "/payments");

		runOnce();
		runOnce();
		runOnce();
		// The listener runs asynchronously after the check's transaction commits.
		await().atMost(Duration.ofSeconds(5)).until(() -> incidentCount(target) == 1);
		assertThat(incident(target)).containsEntry("status", "OPEN")
			.containsEntry("last_error", "Unexpected status 503");

		service.stubFor(get("/payments").willReturn(aResponse().withStatus(200)));
		runOnce();
		runOnce();
		await().atMost(Duration.ofSeconds(5)).until(() -> "RESOLVED".equals(incident(target).get("status")));
		assertThat(incident(target)).containsEntry("resolution", "RECOVERED");
		assertThat(timeline(target)).containsExactly("OPENED by pulsiris", "RESOLVED by pulsiris");
	}

	@Test
	void recoveryAfterAMonitoringGapIsMarkedApproximate() {
		service.stubFor(get("/catalog").willReturn(aResponse().withStatus(503)));
		UUID target = register("Catalog", "/catalog");
		runOnce();
		runOnce();
		runOnce();
		await().atMost(Duration.ofSeconds(5)).until(() -> incidentCount(target) == 1);

		// Pulsiris is offline for two hours; the service recovers at some unknown point.
		clock.advance(Duration.ofHours(2));
		service.stubFor(get("/catalog").willReturn(aResponse().withStatus(200)));
		runOnce();
		runOnce();

		await().atMost(Duration.ofSeconds(5)).until(() -> "RESOLVED".equals(incident(target).get("status")));
		assertThat(timeline(target)).containsExactly("OPENED by pulsiris", "NOTE by pulsiris", "RESOLVED by pulsiris");
	}

	@Test
	void removingATargetResolvesItsIncident() {
		service.stubFor(get("/legacy").willReturn(aResponse().withStatus(500)));
		UUID target = register("Legacy", "/legacy");
		runOnce();
		runOnce();
		runOnce();
		await().atMost(Duration.ofSeconds(5)).until(() -> incidentCount(target) == 1);

		assertThat(mvc.delete().uri("/api/targets/{id}", target)).hasStatus2xxSuccessful();

		await().atMost(Duration.ofSeconds(5))
			.untilAsserted(() -> assertThat(incident(target)).containsEntry("status", "RESOLVED")
				.containsEntry("resolution", "TARGET_REMOVED"));
	}

	@Test
	void aRedeliveredOutageEventDoesNotOpenASecondIncident() {
		UUID target = UUID.randomUUID();

		incidents.openForOutage(target, "Search", T0, T0.plusSeconds(30), "Timed out");
		incidents.openForOutage(target, "Search", T0, T0.plusSeconds(30), "Timed out");
		assertThat(incidentCount(target)).isOne();

		// Even after the incident is resolved, the same outage must not open a new one.
		incidents.resolveRecovered(target, T0, T0.plusSeconds(90), Duration.ZERO);
		incidents.openForOutage(target, "Search", T0, T0.plusSeconds(30), "Timed out");
		assertThat(incidentCount(target)).isOne();
	}

	@Test
	void aNewOutageAfterRecoveryOpensANewIncident() {
		UUID target = UUID.randomUUID();
		incidents.openForOutage(target, "Auth", T0, T0.plusSeconds(30), "Connection refused");
		incidents.resolveRecovered(target, T0, T0.plusSeconds(90), Duration.ZERO);

		incidents.openForOutage(target, "Auth", T0.plusSeconds(600), T0.plusSeconds(630), "Connection refused");

		assertThat(incidentCount(target)).isEqualTo(2);
		assertThat(incident(target)).containsEntry("status", "OPEN");
	}

	@Test
	void aLateRecoveryFromAnEarlierOutageLeavesTheCurrentIncidentOpen() {
		UUID target = UUID.randomUUID();
		incidents.openForOutage(target, "Mail", T0.plusSeconds(600), T0.plusSeconds(630), "Unexpected status 502");

		incidents.resolveRecovered(target, T0, T0.plusSeconds(90), Duration.ZERO);

		assertThat(incident(target)).containsEntry("status", "OPEN");
	}

	@Test
	void recoveryWithoutAnIncidentIsIgnored() {
		UUID target = UUID.randomUUID();

		incidents.resolveRecovered(target, T0, T0.plusSeconds(90), Duration.ZERO);
		incidents.resolveRemovedTarget(target, T0.plusSeconds(90));

		assertThat(incidentCount(target)).isZero();
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

	private int incidentCount(UUID target) {
		return jdbc.queryForObject("select count(*) from incidents.incident where target_id = ?", Integer.class,
				target);
	}

	/** The target's most recently opened incident. */
	private Map<String, Object> incident(UUID target) {
		return jdbc.queryForMap("""
				select status::text as status, resolution::text as resolution, last_error
				from incidents.incident where target_id = ? order by opened_at desc limit 1
				""", target);
	}

	private List<String> timeline(UUID target) {
		return jdbc.queryForList("""
				select e.entry_type || ' by ' || e.actor
				from incidents.incident_timeline_entry e join incidents.incident i on i.id = e.incident_id
				where i.target_id = ? order by e.occurred_at, e.id
				""", String.class, target);
	}

}
