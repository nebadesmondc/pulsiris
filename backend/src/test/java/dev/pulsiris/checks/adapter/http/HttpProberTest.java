package dev.pulsiris.checks.adapter.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.headRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import dev.pulsiris.checks.domain.CheckOutcome;
import dev.pulsiris.checks.domain.CheckResult;
import dev.pulsiris.checks.domain.ProbeMethod;
import dev.pulsiris.checks.domain.ProbeTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HttpProberTest {

	@RegisterExtension
	static WireMockExtension server = WireMockExtension.newInstance()
		.options(wireMockConfig().dynamicPort().dynamicHttpsPort())
		.build();

	private static final Instant NOW = Instant.parse("2026-10-27T09:00:00Z");

	private final HttpProber prober = new HttpProber(Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void healthyEndpointIsASuccess() {
		server.stubFor(get("/health").willReturn(aResponse().withStatus(200)));

		CheckResult result = prober.probe(target("/health"));

		assertThat(result.outcome()).isEqualTo(CheckOutcome.SUCCESS);
		assertThat(result.httpStatus()).isEqualTo(200);
		assertThat(result.checkedAt()).isEqualTo(NOW);
		assertThat(result.latency()).isPositive();
		assertThat(result.errorMessage()).isNull();
	}

	@Test
	void anyTwoHundredCountsWhenNoStatusIsExpected() {
		server.stubFor(get("/ready").willReturn(aResponse().withStatus(204)));

		assertThat(prober.probe(target("/ready")).outcome()).isEqualTo(CheckOutcome.SUCCESS);
	}

	@Test
	void expectedStatusMustMatchExactly() {
		server.stubFor(get("/ready").willReturn(aResponse().withStatus(204)));

		CheckResult result = prober.probe(target("/ready", ProbeMethod.GET, 200, Duration.ofSeconds(2)));

		assertThat(result.outcome()).isEqualTo(CheckOutcome.FAILURE);
		assertThat(result.errorMessage()).isEqualTo("Unexpected status 204");
	}

	@Test
	void serverErrorIsAFailureWithItsStatus() {
		server.stubFor(get("/health").willReturn(aResponse().withStatus(503)));

		CheckResult result = prober.probe(target("/health"));

		assertThat(result.outcome()).isEqualTo(CheckOutcome.FAILURE);
		assertThat(result.httpStatus()).isEqualTo(503);
	}

	@Test
	void slowResponseIsATimeout() {
		server.stubFor(get("/slow").willReturn(aResponse().withStatus(200).withFixedDelay(2_000)));

		CheckResult result = prober.probe(target("/slow", ProbeMethod.GET, null, Duration.ofMillis(300)));

		assertThat(result.outcome()).isEqualTo(CheckOutcome.TIMEOUT);
		assertThat(result.httpStatus()).isNull();
		assertThat(result.errorMessage()).isEqualTo("No response within 300 ms");
		assertThat(result.latency()).isLessThan(Duration.ofSeconds(2));
	}

	@Test
	void droppedConnectionIsAnError() {
		server.stubFor(get("/health").willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

		CheckResult result = prober.probe(target("/health"));

		assertThat(result.outcome()).isEqualTo(CheckOutcome.ERROR);
		assertThat(result.errorMessage()).startsWith("I/O error");
	}

	@Test
	void nothingListeningIsConnectionRefused() throws IOException {
		int closedPort;
		try (ServerSocket socket = new ServerSocket(0)) {
			closedPort = socket.getLocalPort();
		}

		CheckResult result = prober.probe(probe(URI.create("http://127.0.0.1:" + closedPort + "/health")));

		assertThat(result.outcome()).isEqualTo(CheckOutcome.ERROR);
		assertThat(result.errorMessage()).isEqualTo("Connection refused");
	}

	@Test
	void unresolvableHostIsReportedAsUnknownHost() {
		// .invalid is reserved (RFC 6761) and never resolves.
		CheckResult result = prober.probe(probe(URI.create("http://pulsiris-test.invalid/health")));

		assertThat(result.outcome()).isEqualTo(CheckOutcome.ERROR);
		assertThat(result.errorMessage()).isEqualTo("Unknown host");
	}

	@Test
	void untrustedCertificateIsATlsError() {
		server.stubFor(get("/health").willReturn(aResponse().withStatus(200)));

		// WireMock's HTTPS port uses a self-signed certificate, which must not be trusted.
		CheckResult result = prober
			.probe(probe(URI.create("https://localhost:" + server.getRuntimeInfo().getHttpsPort() + "/health")));

		assertThat(result.outcome()).isEqualTo(CheckOutcome.ERROR);
		assertThat(result.errorMessage()).startsWith("TLS error");
	}

	@Test
	void followsRedirects() {
		server.stubFor(get("/old").willReturn(aResponse().withStatus(301).withHeader("Location", "/new")));
		server.stubFor(get("/new").willReturn(aResponse().withStatus(200)));

		assertThat(prober.probe(target("/old")).outcome()).isEqualTo(CheckOutcome.SUCCESS);
	}

	@Test
	void sendsConfiguredMethodAndIdentifiesItself() {
		server.stubFor(head(urlEqualTo("/health")).willReturn(aResponse().withStatus(200)));

		prober.probe(target("/health", ProbeMethod.HEAD, null, Duration.ofSeconds(2)));

		server.verify(headRequestedFor(urlEqualTo("/health")).withHeader("User-Agent", equalTo("Pulsiris")));
	}

	private static ProbeTarget target(String path) {
		return target(path, ProbeMethod.GET, null, Duration.ofSeconds(2));
	}

	private static ProbeTarget target(String path, ProbeMethod method, Integer expectedStatus, Duration timeout) {
		// The HTTP base URL explicitly: baseUrl() returns the HTTPS one once HTTPS is enabled.
		URI url = URI.create(server.getRuntimeInfo().getHttpBaseUrl() + path);
		return new ProbeTarget(UUID.randomUUID(), url, method, expectedStatus, timeout);
	}

	private static ProbeTarget probe(URI url) {
		return new ProbeTarget(UUID.randomUUID(), url, ProbeMethod.GET, null, Duration.ofSeconds(2));
	}

}
