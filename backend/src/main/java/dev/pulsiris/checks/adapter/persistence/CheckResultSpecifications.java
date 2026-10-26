package dev.pulsiris.checks.adapter.persistence;

import java.util.ArrayList;
import java.util.UUID;

import dev.pulsiris.checks.application.CheckFilter;

import org.springframework.data.jpa.domain.PredicateSpecification;

/**
 * Translates a target id and a {@link CheckFilter} into JPA criteria.
 */
final class CheckResultSpecifications {

	private CheckResultSpecifications() {
	}

	static PredicateSpecification<CheckResultEntity> matching(UUID targetId, CheckFilter filter) {
		var criteria = new ArrayList<PredicateSpecification<CheckResultEntity>>();
		criteria.add((from, cb) -> cb.equal(from.get("targetId"), targetId));
		if (filter.outcome() != null) {
			criteria.add((from, cb) -> cb.equal(from.get("outcome"), filter.outcome()));
		}
		if (filter.from() != null) {
			criteria.add((from, cb) -> cb.greaterThanOrEqualTo(from.get("checkedAt"), filter.from()));
		}
		if (filter.to() != null) {
			criteria.add((from, cb) -> cb.lessThan(from.get("checkedAt"), filter.to()));
		}
		return PredicateSpecification.allOf(criteria);
	}

}
