package dev.pulsiris.registry.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import dev.pulsiris.shared.ValidationException;
import dev.pulsiris.shared.Violation;
import org.junit.jupiter.api.Test;

class TargetTest {

	private static final Instant NOW = Instant.parse("2026-10-24T10:00:00Z");

	private static final TargetPolicy POLICY = new TargetPolicy(
			new TargetPolicy.Defaults(HttpMethod.GET, Duration.ofSeconds(60), Duration.ofSeconds(5), 3, 2),
			new TargetPolicy.Limits(Duration.ofSeconds(10), Duration.ofHours(1), Duration.ofMillis(500),
					Duration.ofSeconds(30), 10));

	@Test
	void appliesPolicyDefaultsToOmittedFields() {
		Target target = Target.register(draft("Checkout API", "https://shop.example.com/health"), POLICY, NOW);

		assertThat(target.name()).isEqualTo("Checkout API");
		assertThat(target.endpointUrl()).isEqualTo(URI.create("https://shop.example.com/health"));
		assertThat(target.httpMethod()).isEqualTo(HttpMethod.GET);
		assertThat(target.expectedStatus()).isNull();
		assertThat(target.interval()).isEqualTo(Duration.ofSeconds(60));
		assertThat(target.timeout()).isEqualTo(Duration.ofSeconds(5));
		assertThat(target.failureThreshold()).isEqualTo(3);
		assertThat(target.recoveryThreshold()).isEqualTo(2);
		assertThat(target.enabled()).isTrue();
		assertThat(target.createdAt()).isEqualTo(NOW);
	}

	@Test
	void keepsExplicitValuesAndTrimsName() {
		var draft = new TargetDraft("  Billing  ", "http://billing.internal:8080/ready", HttpMethod.HEAD, 204, 30,
				2000, 5, 1, false);

		Target target = Target.register(draft, POLICY, NOW);

		assertThat(target.name()).isEqualTo("Billing");
		assertThat(target.httpMethod()).isEqualTo(HttpMethod.HEAD);
		assertThat(target.expectedStatus()).isEqualTo(204);
		assertThat(target.interval()).isEqualTo(Duration.ofSeconds(30));
		assertThat(target.timeout()).isEqualTo(Duration.ofMillis(2000));
		assertThat(target.failureThreshold()).isEqualTo(5);
		assertThat(target.recoveryThreshold()).isEqualTo(1);
		assertThat(target.enabled()).isFalse();
	}

	@Test
	void reportsEveryViolationAtOnce() {
		var draft = new TargetDraft(" ", "ftp://files.example.com", null, 42, 5, null, 0, 11, null);

		assertThatThrownBy(() -> Target.register(draft, POLICY, NOW))
			.isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.violations()).containsExactly(
					new Violation("name", "must not be blank"),
					new Violation("endpointUrl", "must use http or https"),
					new Violation("expectedStatus", "must be between 100 and 599"),
					new Violation("intervalSeconds", "must be between 10s and 1h"),
					new Violation("failureThreshold", "must be between 1 and 10"),
					new Violation("recoveryThreshold", "must be between 1 and 10")));
	}

	@Test
	void rejectsUrlsWithCredentials() {
		assertViolation(draft("Admin", "https://user:secret@admin.example.com"),
				new Violation("endpointUrl", "must not contain credentials"));
	}

	@Test
	void rejectsRelativeUrls() {
		assertViolation(draft("Relative", "/health"), new Violation("endpointUrl", "must use http or https"));
	}

	@Test
	void rejectsTimeoutThatIsNotShorterThanInterval() {
		var draft = new TargetDraft("Slow", "https://slow.example.com", null, null, 10, 10_000, null, null, null);

		assertViolation(draft, new Violation("timeoutMs", "must be shorter than the check interval"));
	}

	@Test
	void generatesTimeOrderedIds() {
		Target first = Target.register(draft("First", "https://a.example.com"), POLICY, NOW);
		Target second = Target.register(draft("Second", "https://b.example.com"), POLICY, NOW);

		assertThat(first.id().version()).isEqualTo(7);
		assertThat(first.id().toString()).isLessThan(second.id().toString());
	}

	@Test
	void updateReplacesSettingsButKeepsIdentityAndCreationTime() {
		Target original = Target.register(new TargetDraft("Orders", "https://orders.example.com", null, null, 30,
				null, 5, null, null), POLICY, NOW);
		Instant later = NOW.plusSeconds(3600);

		Target updated = original.update(draft("Orders v2", "https://orders.example.com/v2"), POLICY, later);

		assertThat(updated.id()).isEqualTo(original.id());
		assertThat(updated.createdAt()).isEqualTo(NOW);
		assertThat(updated.updatedAt()).isEqualTo(later);
		assertThat(updated.name()).isEqualTo("Orders v2");
		// Full replacement: omitted fields return to the defaults.
		assertThat(updated.interval()).isEqualTo(Duration.ofSeconds(60));
		assertThat(updated.failureThreshold()).isEqualTo(3);
	}

	@Test
	void updateUsesTheSameValidationAsRegistration() {
		Target original = Target.register(draft("Orders", "https://orders.example.com"), POLICY, NOW);

		assertThatThrownBy(() -> original.update(draft("", "https://orders.example.com"), POLICY, NOW))
			.isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.violations())
				.containsExactly(new Violation("name", "must not be blank")));
	}

	@Test
	void policyRejectsDefaultsOutsideItsLimits() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TargetPolicy(
				new TargetPolicy.Defaults(HttpMethod.GET, Duration.ofSeconds(5), Duration.ofSeconds(1), 3, 2),
				POLICY.limits()));
	}

	private static TargetDraft draft(String name, String url) {
		return new TargetDraft(name, url, null, null, null, null, null, null, null);
	}

	private static void assertViolation(TargetDraft draft, Violation expected) {
		assertThatThrownBy(() -> Target.register(draft, POLICY, NOW))
			.isInstanceOfSatisfying(ValidationException.class,
					ex -> assertThat(ex.violations()).containsExactly(expected));
	}

}
