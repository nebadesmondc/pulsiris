package dev.pulsiris.checks.adapter.persistence;

import java.util.UUID;

import dev.pulsiris.checks.application.CheckFilter;
import dev.pulsiris.checks.application.CheckResultRepository;
import dev.pulsiris.checks.domain.CheckResult;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class JpaCheckResultRepository implements CheckResultRepository {

	private final CheckResultJpaRepository jpa;

	private final ChecksEntityMapper mapper;

	JpaCheckResultRepository(CheckResultJpaRepository jpa, ChecksEntityMapper mapper) {
		this.jpa = jpa;
		this.mapper = mapper;
	}

	@Override
	public void save(CheckResult result) {
		jpa.save(mapper.toEntity(result));
	}

	@Override
	public Page<CheckResult> findByTarget(UUID targetId, CheckFilter filter, Pageable pageable) {
		return jpa.findAll(Specification.where(CheckResultSpecifications.matching(targetId, filter)), pageable)
			.map(mapper::toDomain);
	}

}
