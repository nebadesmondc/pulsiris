package dev.pulsiris.registry.config;

import java.time.Duration;

import dev.pulsiris.registry.domain.HttpMethod;
import dev.pulsiris.registry.domain.TargetPolicy;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code pulsiris.targets.*} from application.yaml or environment variables.
 */
@ConfigurationProperties("pulsiris.targets")
record TargetProperties(Defaults defaults, Limits limits) {

	record Defaults(HttpMethod httpMethod, Duration interval, Duration timeout, int failureThreshold,
			int recoveryThreshold) {
	}

	record Limits(Duration minInterval, Duration maxInterval, Duration minTimeout, Duration maxTimeout,
			int maxThreshold) {
	}

	TargetPolicy toPolicy() {
		return new TargetPolicy(
				new TargetPolicy.Defaults(defaults.httpMethod(), defaults.interval(), defaults.timeout(),
						defaults.failureThreshold(), defaults.recoveryThreshold()),
				new TargetPolicy.Limits(limits.minInterval(), limits.maxInterval(), limits.minTimeout(),
						limits.maxTimeout(), limits.maxThreshold()));
	}

}
