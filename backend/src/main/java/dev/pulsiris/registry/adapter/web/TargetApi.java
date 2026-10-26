package dev.pulsiris.registry.adapter.web;

import java.util.UUID;

import dev.pulsiris.registry.application.TargetFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * HTTP contract of the targets API: routes plus OpenAPI documentation. {@link TargetController}
 * implements the behaviour.
 */
@Tag(name = "Targets", description = "Services that Pulsiris monitors")
@RequestMapping("/api/targets")
interface TargetApi {

	@Operation(summary = "Register a target", description = "Omitted settings receive the configured defaults.")
	@ApiResponse(responseCode = "201", description = "Registered. The Location header points to the new target.")
	@ApiResponse(responseCode = "400", description = "Invalid settings",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Name already in use",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@PostMapping
	ResponseEntity<TargetResponse> register(@RequestBody TargetRequest request);

	@Operation(summary = "List targets",
			description = "Filters are optional and combined. Sortable by name, createdAt and updatedAt.")
	@ApiResponse(responseCode = "200", description = "One page of targets")
	@ApiResponse(responseCode = "400", description = "Unsupported sort property",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@GetMapping
	PagedModel<TargetResponse> list(@ParameterObject @ModelAttribute TargetFilter filter,
			@ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable);

	@Operation(summary = "Get a target")
	@ApiResponse(responseCode = "200", description = "The target")
	@ApiResponse(responseCode = "404", description = "No target with this id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@GetMapping("/{id}")
	TargetResponse get(@PathVariable UUID id);

	@Operation(summary = "Replace a target's settings",
			description = "Full replacement: omitted settings return to the configured defaults.")
	@ApiResponse(responseCode = "200", description = "The updated target")
	@ApiResponse(responseCode = "400", description = "Invalid settings",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "404", description = "No target with this id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@ApiResponse(responseCode = "409", description = "Name already in use",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@PutMapping("/{id}")
	TargetResponse update(@PathVariable UUID id, @RequestBody TargetRequest request);

	@Operation(summary = "Remove a target")
	@ApiResponse(responseCode = "204", description = "Removed")
	@ApiResponse(responseCode = "404", description = "No target with this id",
			content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
					schema = @Schema(implementation = ProblemDetail.class)))
	@DeleteMapping("/{id}")
	ResponseEntity<Void> remove(@PathVariable UUID id);

}
