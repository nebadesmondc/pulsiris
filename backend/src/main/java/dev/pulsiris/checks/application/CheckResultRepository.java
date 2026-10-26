package dev.pulsiris.checks.application;

import dev.pulsiris.checks.domain.CheckResult;

public interface CheckResultRepository {

	void save(CheckResult result);

}
