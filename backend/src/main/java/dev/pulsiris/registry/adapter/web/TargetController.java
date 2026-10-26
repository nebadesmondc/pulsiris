package dev.pulsiris.registry.adapter.web;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import dev.pulsiris.registry.application.TargetFilter;
import dev.pulsiris.registry.application.TargetService;
import dev.pulsiris.registry.domain.Target;
import dev.pulsiris.shared.ValidationException;
import dev.pulsiris.shared.Violation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
class TargetController implements TargetApi {

	private static final Set<String> SORTABLE = Set.of("name", "createdAt", "updatedAt");

	private final TargetService targets;

	TargetController(TargetService targets) {
		this.targets = targets;
	}

	@Override
	public ResponseEntity<TargetResponse> register(TargetRequest request) {
		Target target = targets.register(request.toDraft());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}")
			.buildAndExpand(target.id())
			.toUri();
		return ResponseEntity.created(location).body(TargetResponse.from(target));
	}

	@Override
	public PagedModel<TargetResponse> list(TargetFilter filter, Pageable pageable) {
		// Only whitelisted properties: anything else would reach the database query.
		pageable.getSort().forEach(order -> {
			if (!SORTABLE.contains(order.getProperty())) {
				throw new ValidationException(List.of(new Violation("sort", "must be one of createdAt, name, updatedAt")));
			}
		});
		return new PagedModel<>(targets.list(filter, pageable).map(TargetResponse::from));
	}

	@Override
	public TargetResponse get(UUID id) {
		return TargetResponse.from(targets.get(id));
	}

	@Override
	public TargetResponse update(UUID id, TargetRequest request) {
		return TargetResponse.from(targets.update(id, request.toDraft()));
	}

	@Override
	public ResponseEntity<Void> remove(UUID id) {
		targets.remove(id);
		return ResponseEntity.noContent().build();
	}

}
