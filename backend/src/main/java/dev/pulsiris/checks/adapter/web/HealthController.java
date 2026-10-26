package dev.pulsiris.checks.adapter.web;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import dev.pulsiris.checks.application.CheckFilter;
import dev.pulsiris.checks.application.HealthQueries;
import dev.pulsiris.shared.ValidationException;
import dev.pulsiris.shared.Violation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HealthController implements HealthApi {

	private static final Set<String> SORTABLE = Set.of("checkedAt", "latencyMs");

	private final HealthQueries queries;

	HealthController(HealthQueries queries) {
		this.queries = queries;
	}

	@Override
	public TargetHealthResponse health(UUID id) {
		return TargetHealthResponse.from(queries.health(id));
	}

	@Override
	public PagedModel<CheckResultResponse> checks(UUID id, CheckFilter filter, Pageable pageable) {
		pageable.getSort().forEach(order -> {
			if (!SORTABLE.contains(order.getProperty())) {
				throw new ValidationException(List.of(new Violation("sort", "must be one of checkedAt, latencyMs")));
			}
		});
		return new PagedModel<>(queries.history(id, filter, pageable).map(CheckResultResponse::from));
	}

}
