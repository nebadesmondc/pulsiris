package dev.pulsiris.checks.domain;

public enum CheckOutcome {
	/** Answered with the expected status. */
	SUCCESS,
	/** Answered, but with an unexpected status. */
	FAILURE,
	/** Did not answer within the timeout. */
	TIMEOUT,
	/** Could not be reached: DNS, connection or TLS problem. */
	ERROR;

	public boolean isSuccess() {
		return this == SUCCESS;
	}

}
