package dev.pulsiris.incidents.adapter.web;

import java.util.UUID;

import dev.pulsiris.incidents.application.IncidentFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * HTTP contract of the incidents API: routes plus OpenAPI documentation. {@link IncidentController}
 * implements the behaviour.
 */
@Tag(name = "Incidents", description = "Outages of monitored targets and the actions taken on them")
@RequestMapping("/api/incidents")
interface IncidentApi {

	@Operation(summary = "List incidents",
			description = "Newest first by default. Filter by status, target and opening time window (from "
					+ "inclusive, to exclusive, ISO-8601). Sortable by openedAt, startedAt and resolvedAt.")
	@ApiResponse(responseCode = "200", description = "One page of incidents")
	@ApiResponse(responseCode = "400", description = "Invalid time window or sort property",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@GetMapping
	PagedModel<IncidentResponse> list(@ParameterObject @ModelAttribute IncidentFilter filter,
			@ParameterObject @PageableDefault(size = 20, sort = "openedAt",
					direction = Sort.Direction.DESC) Pageable pageable);

	@Operation(summary = "Get an incident with its timeline")
	@ApiResponse(responseCode = "200", description = "The incident, timeline oldest first")
	@ApiResponse(responseCode = "404", description = "No incident with this id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@GetMapping("/{id}")
	IncidentDetailsResponse get(@PathVariable UUID id);

	@Operation(summary = "Acknowledge an incident", description = "Only an OPEN incident can be acknowledged.")
	@ApiResponse(responseCode = "200", description = "The acknowledged incident")
	@ApiResponse(responseCode = "400", description = "Missing actor",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "No incident with this id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Already acknowledged or resolved, or changed concurrently",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@PostMapping("/{id}/acknowledge")
	IncidentDetailsResponse acknowledge(@PathVariable UUID id,
			@RequestBody IncidentActionRequests.Acknowledge request);

	@Operation(summary = "Resolve an incident manually",
			description = "Incidents resolve on their own when the target recovers. Resolving by hand while the "
					+ "target is still down means no new incident opens until it recovers and fails again, so "
					+ "a reason is required.")
	@ApiResponse(responseCode = "200", description = "The resolved incident")
	@ApiResponse(responseCode = "400", description = "Missing actor or reason",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "No incident with this id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Already resolved, or changed concurrently",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@PostMapping("/{id}/resolve")
	IncidentDetailsResponse resolve(@PathVariable UUID id, @RequestBody IncidentActionRequests.Resolve request);

	@Operation(summary = "Add a note to the timeline", description = "Allowed in every status.")
	@ApiResponse(responseCode = "200", description = "The incident with the new note")
	@ApiResponse(responseCode = "400", description = "Missing actor or message",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "No incident with this id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@PostMapping("/{id}/notes")
	IncidentDetailsResponse addNote(@PathVariable UUID id, @RequestBody IncidentActionRequests.Note request);

}
