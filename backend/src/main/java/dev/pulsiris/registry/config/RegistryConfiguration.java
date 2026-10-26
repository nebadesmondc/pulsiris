package dev.pulsiris.registry.config;

import dev.pulsiris.registry.domain.TargetPolicy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TargetProperties.class)
class RegistryConfiguration {

	// Fails at startup if the configured defaults fall outside the configured limits.
	@Bean
	TargetPolicy targetPolicy(TargetProperties properties) {
		return properties.toPolicy();
	}

}
