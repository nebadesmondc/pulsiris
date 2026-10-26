package dev.pulsiris.registry.adapter.web;

import dev.pulsiris.registry.domain.HttpMethod;
import dev.pulsiris.registry.domain.TargetDraft;
import io.swagger.v3.oas.annotations.media.Schema;

record TargetRequest(
		@Schema(description = "Unique, case-insensitive name used in alerts", example = "Checkout API",
				requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
		String name,
		@Schema(description = "http or https URL to check", example = "https://shop.example.com/health",
				requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2048)
		String endpointUrl,
		@Schema(description = "Defaults to GET") HttpMethod httpMethod,
		@Schema(description = "Exact status that counts as healthy. Omit to accept any 2xx.", example = "200")
		Integer expectedStatus,
		@Schema(description = "Seconds between checks. Defaults to the configured interval.", example = "60")
		Integer intervalSeconds,
		@Schema(description = "Milliseconds before a check fails. Must be shorter than the interval.", example = "5000")
		Integer timeoutMs,
		@Schema(description = "Consecutive failures before the target is DOWN", example = "3") Integer failureThreshold,
		@Schema(description = "Consecutive successes before a DOWN target recovers", example = "2")
		Integer recoveryThreshold,
		@Schema(description = "Disabled targets are not checked. Defaults to true.") Boolean enabled) {

	TargetDraft toDraft() {
		return new TargetDraft(name, endpointUrl, httpMethod, expectedStatus, intervalSeconds, timeoutMs,
				failureThreshold, recoveryThreshold, enabled);
	}

}
