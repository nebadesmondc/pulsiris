package dev.pulsiris.shared.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API-wide OpenAPI metadata. Per-endpoint documentation lives on each module's API interface.
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfiguration {

	@Bean
	OpenAPI pulsirisOpenApi(ObjectProvider<BuildProperties> build) {
		String version = build.getIfAvailable() != null ? build.getIfAvailable().getVersion() : "dev";
		return new OpenAPI().info(new Info().title("Pulsiris API")
			.version(version)
			.description("Register services, follow their health and manage incidents."));
	}

}
