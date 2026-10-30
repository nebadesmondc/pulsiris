package dev.pulsiris.incidents.application;

import java.util.List;
import java.util.UUID;

import dev.pulsiris.incidents.domain.Incident;
import dev.pulsiris.shared.ValidationException;
import dev.pulsiris.shared.Violation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the incidents module.
 */
@Service
@Transactional(readOnly = true)
public class IncidentQueries {

	private final IncidentRepository incidents;

	IncidentQueries(IncidentRepository incidents) {
		this.incidents = incidents;
	}

	public Page<Incident> list(IncidentFilter filter, Pageable pageable) {
		if (filter.from() != null && filter.to() != null && !filter.from().isBefore(filter.to())) {
			throw new ValidationException(List.of(new Violation("from", "must be before to")));
		}
		return incidents.findAll(filter, pageable);
	}

	public IncidentDetails get(UUID id) {
		Incident incident = incidents.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
		return new IncidentDetails(incident, incidents.findTimeline(id));
	}

}
