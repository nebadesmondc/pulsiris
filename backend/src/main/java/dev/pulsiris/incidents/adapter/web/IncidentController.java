package dev.pulsiris.incidents.adapter.web;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import dev.pulsiris.incidents.application.IncidentFilter;
import dev.pulsiris.incidents.application.IncidentQueries;
import dev.pulsiris.incidents.application.IncidentService;
import dev.pulsiris.shared.ValidationException;
import dev.pulsiris.shared.Violation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.RestController;

@RestController
class IncidentController implements IncidentApi {

	private static final Set<String> SORTABLE = Set.of("openedAt", "startedAt", "resolvedAt");

	private final IncidentService service;

	private final IncidentQueries queries;

	IncidentController(IncidentService service, IncidentQueries queries) {
		this.service = service;
		this.queries = queries;
	}

	@Override
	public PagedModel<IncidentResponse> list(IncidentFilter filter, Pageable pageable) {
		pageable.getSort().forEach(order -> {
			if (!SORTABLE.contains(order.getProperty())) {
				throw new ValidationException(
						List.of(new Violation("sort", "must be one of openedAt, startedAt, resolvedAt")));
			}
		});
		return new PagedModel<>(queries.list(filter, pageable).map(IncidentResponse::from));
	}

	@Override
	public IncidentDetailsResponse get(UUID id) {
		return IncidentDetailsResponse.from(queries.get(id));
	}

	@Override
	public IncidentDetailsResponse acknowledge(UUID id, IncidentActionRequests.Acknowledge request) {
		service.acknowledge(id, request.actor());
		return get(id);
	}

	@Override
	public IncidentDetailsResponse resolve(UUID id, IncidentActionRequests.Resolve request) {
		service.resolve(id, request.actor(), request.reason());
		return get(id);
	}

	@Override
	public IncidentDetailsResponse addNote(UUID id, IncidentActionRequests.Note request) {
		service.addNote(id, request.actor(), request.message());
		return get(id);
	}

}
