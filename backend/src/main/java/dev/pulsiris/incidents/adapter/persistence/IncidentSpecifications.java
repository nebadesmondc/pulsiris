package dev.pulsiris.incidents.adapter.persistence;

import java.util.ArrayList;

import dev.pulsiris.incidents.application.IncidentFilter;

import org.springframework.data.jpa.domain.PredicateSpecification;

/**
 * Translates an {@link IncidentFilter} into JPA criteria.
 */
final class IncidentSpecifications {

	private IncidentSpecifications() {
	}

	static PredicateSpecification<IncidentEntity> matching(IncidentFilter filter) {
		var criteria = new ArrayList<PredicateSpecification<IncidentEntity>>();
		if (filter.status() != null) {
			criteria.add((from, cb) -> cb.equal(from.get("status"), filter.status()));
		}
		if (filter.targetId() != null) {
			criteria.add((from, cb) -> cb.equal(from.get("targetId"), filter.targetId()));
		}
		if (filter.from() != null) {
			criteria.add((from, cb) -> cb.greaterThanOrEqualTo(from.get("openedAt"), filter.from()));
		}
		if (filter.to() != null) {
			criteria.add((from, cb) -> cb.lessThan(from.get("openedAt"), filter.to()));
		}
		return PredicateSpecification.allOf(criteria);
	}

}
