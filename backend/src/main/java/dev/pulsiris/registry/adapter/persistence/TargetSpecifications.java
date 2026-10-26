package dev.pulsiris.registry.adapter.persistence;

import java.util.ArrayList;

import dev.pulsiris.registry.application.TargetFilter;

import org.springframework.data.jpa.domain.PredicateSpecification;

/**
 * Translates a {@link TargetFilter} into JPA criteria. Each criterion is only applied when set.
 */
final class TargetSpecifications {

	private TargetSpecifications() {
	}

	static PredicateSpecification<TargetEntity> matching(TargetFilter filter) {
		var criteria = new ArrayList<PredicateSpecification<TargetEntity>>();
		if (filter.name() != null && !filter.name().isBlank()) {
			criteria.add(nameContains(filter.name().strip()));
		}
		if (filter.enabled() != null) {
			criteria.add(enabledIs(filter.enabled()));
		}
		return criteria.isEmpty() ? PredicateSpecification.unrestricted() : PredicateSpecification.allOf(criteria);
	}

	static PredicateSpecification<TargetEntity> nameContains(String text) {
		// Escape LIKE wildcards so "50%" matches literally instead of "50 followed by anything".
		String pattern = "%" + text.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
		return (from, cb) -> cb.like(cb.lower(from.get("name")), pattern, '\\');
	}

	static PredicateSpecification<TargetEntity> enabledIs(boolean enabled) {
		return (from, cb) -> cb.equal(from.get("enabled"), enabled);
	}

}
