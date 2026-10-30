package dev.pulsiris.incidents;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import dev.pulsiris.IntegrationTest;
import dev.pulsiris.MutableClock;
import dev.pulsiris.TestClockConfiguration;
import dev.pulsiris.incidents.application.IncidentService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
@Import(TestClockConfiguration.class)
class IncidentApiTests {

	@Autowired
	MockMvcTester mvc;

	@Autowired
	IncidentService incidents;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void showsAnIncidentWithItsTimeline() {
		UUID id = openIncident(UUID.randomUUID());

		MvcTestResult result = mvc.get().uri("/api/incidents/{id}", id).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.incident.status").isEqualTo("OPEN");
		assertThat(result).bodyJson().extractingPath("$.incident.lastError").isEqualTo("Unexpected status 503");
		assertThat(result).bodyJson().extractingPath("$.timeline[0].type").isEqualTo("OPENED");
		assertThat(result).bodyJson().extractingPath("$.timeline[0].actor").isEqualTo("pulsiris");
	}

	@Test
	void unknownIncidentIsNotFound() {
		assertThat(mvc.get().uri("/api/incidents/{id}", UUID.randomUUID())).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void listsIncidentsFilteredByTargetAndStatus() {
		UUID target = UUID.randomUUID();
		UUID first = openIncident(target);
		incidents.resolveRecovered(target, startOf(first), clock.instant());
		UUID second = openIncident(target);

		MvcTestResult all = mvc.get().uri("/api/incidents?targetId={target}", target).exchange();
		assertThat(all).hasStatusOk();
		assertThat(all).bodyJson()
			.extractingPath("$.content[*].id")
			.asArray()
			.containsExactly(second.toString(), first.toString());

		MvcTestResult open = mvc.get().uri("/api/incidents?targetId={target}&status=OPEN", target).exchange();
		assertThat(open).bodyJson().extractingPath("$.content[*].id").asArray().containsExactly(second.toString());
	}

	@Test
	void rejectsUnsupportedSortAndEmptyTimeWindow() {
		assertThat(mvc.get().uri("/api/incidents?sort=targetName")).hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("sort");
		assertThat(mvc.get().uri("/api/incidents?from=2026-11-02T10:00:00Z&to=2026-11-02T09:00:00Z"))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.bodyJson()
			.extractingPath("$.errors[0].field")
			.isEqualTo("from");
	}

	@Test
	void acknowledgesOnceAndRecordsTheActor() {
		UUID id = openIncident(UUID.randomUUID());
		clock.advance(Duration.ofMinutes(4));

		MvcTestResult result = post("/api/incidents/{id}/acknowledge", id, """
				{ "actor": "user-x" }
				""");
		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.incident.status").isEqualTo("ACKNOWLEDGED");
		assertThat(result).bodyJson().extractingPath("$.incident.timeToAcknowledgeSeconds").isEqualTo(240);
		assertThat(result).bodyJson().extractingPath("$.timeline[1].actor").isEqualTo("user-x");

		assertThat(post("/api/incidents/{id}/acknowledge", id, """
				{ "actor": "user-y" }
				""")).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void manualResolveNeedsAReason() {
		UUID id = openIncident(UUID.randomUUID());

		assertThat(post("/api/incidents/{id}/resolve", id, """
				{ "actor": "user-x" }
				""")).hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.errors[0].field").isEqualTo("reason");

		MvcTestResult result = post("/api/incidents/{id}/resolve", id, """
				{ "actor": "user-x", "reason": "Planned maintenance" }
				""");
		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.incident.resolution").isEqualTo("MANUAL");
		assertThat(result).bodyJson().extractingPath("$.timeline[1].message").isEqualTo("Planned maintenance");

		assertThat(post("/api/incidents/{id}/resolve", id, """
				{ "actor": "user-x", "reason": "Again" }
				""")).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void notesCanBeAddedAfterResolution() {
		UUID target = UUID.randomUUID();
		UUID id = openIncident(target);
		incidents.resolveRecovered(target, startOf(id), clock.instant());

		MvcTestResult result = post("/api/incidents/{id}/notes", id, """
				{ "actor": "user-x", "message": "Root cause: expired certificate" }
				""");

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.timeline[*].type").asArray()
			.containsExactly("OPENED", "RESOLVED", "NOTE");
		assertThat(post("/api/incidents/{id}/notes", id, """
				{ "actor": "user-x", "message": " " }
				""")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	/** Opens an incident the way the health event would, starting two minutes before now. */
	private UUID openIncident(UUID target) {
		// Keeps opening times distinct, so the newest-first order is deterministic.
		clock.advance(Duration.ofSeconds(1));
		Instant now = clock.instant();
		incidents.openForOutage(target, "Checkout", now.minusSeconds(120), now, "Unexpected status 503");
		return jdbc.queryForObject(
				"select id from incidents.incident where target_id = ? and status <> 'RESOLVED'", UUID.class, target);
	}

	private Instant startOf(UUID incident) {
		return jdbc.queryForObject("select started_at from incidents.incident where id = ?", Instant.class,
				incident);
	}

	private MvcTestResult post(String uri, UUID id, String json) {
		return mvc.post().uri(uri, id).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
	}

}
