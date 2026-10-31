package dev.pulsiris.incidents.adapter.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request bodies of the incident actions. Until authentication exists, the caller names the actor.
 */
final class IncidentActionRequests {

	private IncidentActionRequests() {
	}

	record Acknowledge(
			@Schema(description = "Who is taking the incident", example = "user-x",
					requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
			String actor) {
	}

	record Resolve(
			@Schema(description = "Who is resolving the incident", example = "user-x",
					requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
			String actor,
			@Schema(description = "Why it can be closed while checks have not confirmed recovery",
					example = "Planned maintenance, service intentionally offline",
					requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 2000)
			String reason) {
	}

	record Note(
			@Schema(description = "Who is writing the note", example = "user-x",
					requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100)
			String actor,
			@Schema(example = "Rolled back release 1.4.2", requiredMode = Schema.RequiredMode.REQUIRED,
					maxLength = 2000)
			String message) {
	}

}
