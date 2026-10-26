package dev.pulsiris;

import java.time.Instant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the application clock with a {@link MutableClock} that tests move forward.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfiguration {

	@Bean
	@Primary
	MutableClock testClock() {
		return new MutableClock(Instant.parse("2026-11-02T09:00:00Z"));
	}

}
