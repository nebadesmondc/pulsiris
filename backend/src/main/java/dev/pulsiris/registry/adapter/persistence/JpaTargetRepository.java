package dev.pulsiris.registry.adapter.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import dev.pulsiris.registry.application.TargetFilter;
import dev.pulsiris.registry.application.TargetRepository;
import dev.pulsiris.registry.domain.DuplicateTargetNameException;
import dev.pulsiris.registry.domain.Target;
import org.hibernate.exception.ConstraintViolationException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class JpaTargetRepository implements TargetRepository {

	private static final String NAME_CONSTRAINT = "monitored_target_name_uq";

	private final TargetJpaRepository jpa;

	private final TargetEntityMapper mapper;

	JpaTargetRepository(TargetJpaRepository jpa, TargetEntityMapper mapper) {
		this.jpa = jpa;
		this.mapper = mapper;
	}

	@Override
	public Target save(Target target) {
		// Update the managed entity when it exists, so its version is kept; otherwise insert.
		TargetEntity entity = jpa.findById(target.id()).map(existing -> {
			mapper.copyOnto(target, existing);
			return existing;
		}).orElseGet(() -> mapper.toEntity(target));
		try {
			// Flush now so a unique-index violation surfaces here, not at commit.
			return mapper.toDomain(jpa.saveAndFlush(entity));
		}
		catch (DataIntegrityViolationException ex) {
			if (ex.getCause() instanceof ConstraintViolationException cve
					&& NAME_CONSTRAINT.equals(cve.getConstraintName())) {
				throw new DuplicateTargetNameException(target.name());
			}
			throw ex;
		}
	}

	@Override
	public Optional<Target> findById(UUID id) {
		return jpa.findById(id).map(mapper::toDomain);
	}

	@Override
	public Page<Target> findAll(TargetFilter filter, Pageable pageable) {
		return jpa.findAll(Specification.where(TargetSpecifications.matching(filter)), sortNamesIgnoringCase(pageable))
			.map(mapper::toDomain);
	}

	@Override
	public List<Target> findAllEnabled() {
		return jpa.findAll(TargetSpecifications.enabledIs(true)).stream().map(mapper::toDomain).toList();
	}

	private static Pageable sortNamesIgnoringCase(Pageable pageable) {
		Sort sort = Sort.by(pageable.getSort()
			.stream()
			.map(order -> order.getProperty().equals("name") ? order.ignoreCase() : order)
			.toList());
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}

	@Override
	public boolean existsByName(String name) {
		return jpa.existsByNameIgnoringCase(name);
	}

	@Override
	public boolean existsByNameExcluding(String name, UUID excludedId) {
		return jpa.existsByNameIgnoringCaseExcluding(name, excludedId);
	}

	@Override
	public void delete(UUID id) {
		jpa.deleteById(id);
	}

}
