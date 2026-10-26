package dev.pulsiris.checks.application;

import java.util.UUID;

import dev.pulsiris.checks.domain.CheckResult;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CheckResultRepository {

	void save(CheckResult result);

	Page<CheckResult> findByTarget(UUID targetId, CheckFilter filter, Pageable pageable);

}
