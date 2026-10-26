package dev.pulsiris.checks.adapter.persistence;

import dev.pulsiris.checks.application.CheckResultRepository;
import dev.pulsiris.checks.domain.CheckResult;

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

}
