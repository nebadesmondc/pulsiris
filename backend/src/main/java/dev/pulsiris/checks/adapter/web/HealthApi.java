package dev.pulsiris.checks.adapter.web;

import java.util.UUID;

import dev.pulsiris.checks.application.CheckFilter;
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
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * HTTP contract of the health API: routes plus OpenAPI documentation. {@link HealthController}
 * implements the behaviour.
 */
@Tag(name = "Health", description = "Current health and check history of targets")
@RequestMapping("/api/targets/{id}")
interface HealthApi {

	@Operation(summary = "Get a target's current health",
			description = "A target that has not been checked yet is UNKNOWN.")
	@ApiResponse(responseCode = "200", description = "Current health")
	@ApiResponse(responseCode = "404", description = "No target with this id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@GetMapping("/health")
	TargetHealthResponse health(@PathVariable UUID id);

	@Operation(summary = "List a target's check results",
			description = "Newest first by default. Filter by outcome and by time window (from inclusive, to "
					+ "exclusive, ISO-8601). Sortable by checkedAt and latencyMs.")
	@ApiResponse(responseCode = "200", description = "One page of check results")
	@ApiResponse(responseCode = "400", description = "Invalid time window or sort property",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "No target with this id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@GetMapping("/checks")
	PagedModel<CheckResultResponse> checks(@PathVariable UUID id, @ParameterObject @ModelAttribute CheckFilter filter,
			@ParameterObject @PageableDefault(size = 20, sort = "checkedAt",
					direction = Sort.Direction.DESC) Pageable pageable);

}
