package dev.pulsiris.checks.adapter.http;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.channels.UnresolvedAddressException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import javax.net.ssl.SSLException;

import dev.pulsiris.checks.application.Prober;
import dev.pulsiris.checks.domain.CheckOutcome;
import dev.pulsiris.checks.domain.CheckResult;
import dev.pulsiris.checks.domain.ProbeTarget;

import org.springframework.stereotype.Component;

/**
 * Probes targets over HTTP(S) with the JDK client: one shared client, a timeout per request,
 * redirects followed and TLS certificates verified. The response body is discarded.
 */
@Component
class HttpProber implements Prober {

	private static final String USER_AGENT = "Pulsiris";

	private static final int MAX_ERROR_LENGTH = 500;

	private final HttpClient client;

	private final Clock clock;

	HttpProber(Clock clock) {
		this.clock = clock;
		this.client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
	}

	@Override
	public CheckResult probe(ProbeTarget target) {
		HttpRequest request = HttpRequest.newBuilder(target.endpointUrl())
			.method(target.method().name(), HttpRequest.BodyPublishers.noBody())
			.timeout(target.timeout())
			.header("User-Agent", USER_AGENT)
			.build();
		Instant checkedAt = clock.instant();
		long start = System.nanoTime();
		try {
			int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
			Duration latency = since(start);
			if (target.accepts(status)) {
				return result(target, checkedAt, CheckOutcome.SUCCESS, status, latency, null);
			}
			return result(target, checkedAt, CheckOutcome.FAILURE, status, latency, "Unexpected status " + status);
		}
		catch (HttpTimeoutException ex) {
			return result(target, checkedAt, CheckOutcome.TIMEOUT, null, since(start),
					"No response within " + target.timeout().toMillis() + " ms");
		}
		catch (IOException ex) {
			return result(target, checkedAt, CheckOutcome.ERROR, null, since(start), describe(ex));
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return result(target, checkedAt, CheckOutcome.ERROR, null, since(start), "Check interrupted");
		}
	}

	private static String describe(IOException ex) {
		if (hasCause(ex, SSLException.class)) {
			return truncate("TLS error: " + rootMessage(ex));
		}
		// The JDK wraps the real reason in one or more ConnectExceptions, so search the whole chain.
		if (hasCause(ex, UnresolvedAddressException.class)) {
			return "Unknown host";
		}
		if (ex instanceof ConnectException) {
			return "Connection refused";
		}
		return truncate("I/O error: " + rootMessage(ex));
	}

	private static boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (type.isInstance(cause)) {
				return true;
			}
		}
		return false;
	}

	private static String rootMessage(Throwable ex) {
		Throwable root = ex;
		while (root.getCause() != null) {
			root = root.getCause();
		}
		return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
	}

	private static String truncate(String message) {
		return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
	}

	private static Duration since(long startNanos) {
		return Duration.ofNanos(System.nanoTime() - startNanos);
	}

	private static CheckResult result(ProbeTarget target, Instant checkedAt, CheckOutcome outcome, Integer status,
			Duration latency, String error) {
		return new CheckResult(target.targetId(), checkedAt, outcome, status, latency, error);
	}

}
