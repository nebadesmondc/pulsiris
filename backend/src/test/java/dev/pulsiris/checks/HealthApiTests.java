package dev.pulsiris.checks;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
@Import(TestClockConfiguration.class)
class HealthApiTests {

	@RegisterExtension
	static WireMockExtension service = WireMockExtension.newInstance().build();

	private static final Duration INTERVAL = Duration.ofSeconds(10);

	@Autowired
	MockMvcTester mvc;

	@Autowired
	CheckRunner runner;

	@Autowired
	MutableClock clock;

	@Test
	void reportsCurrentHealthOfADownTarget() {
		service.stubFor(get("/payments").willReturn(aResponse().withStatus(503)));
		UUID id = register("/payments");
		Instant firstFailure = clock.instant();
		runChecks(3);

		MvcTestResult result = mvc.get().uri("/api/targets/{id}/health", id).exchange();

		assertThat(result).hasStatusOk();
		assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("DOWN");
		assertThat(result).bodyJson().extractingPath("$.consecutiveFailures").isEqualTo(3);
		assertThat(result).bodyJson().extractingPath("$.failingSince").isEqualTo(firstFailure.toString());
		assertThat(result).bodyJson().extractingPath("$.enabled").isEqualTo(true);
	}

	@Test
	void targetThatWasNeverCheckedIsUnknown() {
		UUID id = register("/new");

		assertThat(mvc.get().uri("/api/targets/{id}/health", id)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.status")
			.isEqualTo("UNKNOWN");
	}

	@Test
	void listsCheckHistoryNewestFirstAndFiltersByOutcome() {
		service.stubFor(get("/search").willReturn(aResponse().withStatus(200)));
		UUID id = register("/search");
		runChecks(2);
		service.stubFor(get("/search").willReturn(aResponse().withStatus(500)));
		runChecks(1);

		MvcTestResult all = mvc.get().uri("/api/targets/{id}/checks", id).exchange();
		assertThat(all).hasStatusOk();
		assertThat(all).bodyJson().extractingPath("$.content[*].outcome")
			.asArray()
			.containsExactly("FAILURE", "SUCCESS", "SUCCESS");
		assertThat(all).bodyJson().extractingPath("$.content[0].httpStatus").isEqualTo(500);

		MvcTestResult failures = mvc.get().uri("/api/targets/{id}/checks?outcome=FAILURE", id).exchange();
		assertThat(failures).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
	}

	@Test
	void filtersCheckHistoryByTimeWindow() {
		service.stubFor(get("/catalog").willReturn(aResponse().withStatus(200)));
		UUID id = register("/catalog");
		runChecks(1);
		Instant from = clock.instant();
		runChecks(2);

		assertThat(mvc.get().uri("/api/targets/{id}/checks?from={from}", id, from)).hasStatusOk()
			.bodyJson()
			.extractingPath("$.page.totalElements")
			.isEqualTo(2);
	}

	@Test
	void rejectsInvalidHistoryQueries() {
		UUID id = register("/admin");
		Instant now = clock.instant();

		assertThat(mvc.get().uri("/api/targets/{id}/checks?from={from}&to={to}", id, now, now))
			.hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/targets/{id}/checks?sort=errorMessage", id)).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void unknownTargetIsNotFound() {
		UUID unknown = UUID.randomUUID();

		assertThat(mvc.get().uri("/api/targets/{id}/health", unknown)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(mvc.get().uri("/api/targets/{id}/checks", unknown)).hasStatus(HttpStatus.NOT_FOUND);
	}

	private UUID register(String path) {
		String json = """
				{ "name": "Service %s", "endpointUrl": "%s%s", "intervalSeconds": %d, "timeoutMs": 1000 }
				""".formatted(UUID.randomUUID().toString().substring(0, 8), service.baseUrl(), path,
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

	private void runChecks(int times) {
		for (int i = 0; i < times; i++) {
			runner.runDueChecks();
			clock.advance(INTERVAL);
		}
	}

}
