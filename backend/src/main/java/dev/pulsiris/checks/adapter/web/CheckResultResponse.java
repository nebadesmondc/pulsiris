package dev.pulsiris.checks.adapter.web;

import java.time.Instant;

import dev.pulsiris.checks.domain.CheckOutcome;
import dev.pulsiris.checks.domain.CheckResult;

record CheckResultResponse(Instant checkedAt, CheckOutcome outcome, Integer httpStatus, long latencyMs,
		String errorMessage) {

	static CheckResultResponse from(CheckResult result) {
		return new CheckResultResponse(result.checkedAt(), result.outcome(), result.httpStatus(),
				result.latency().toMillis(), result.errorMessage());
	}

}
