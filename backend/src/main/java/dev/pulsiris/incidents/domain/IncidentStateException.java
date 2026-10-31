package dev.pulsiris.incidents.domain;

import dev.pulsiris.shared.ConflictException;

/**
 * The action is not allowed in the incident's current status.
 */
public class IncidentStateException extends ConflictException {

	public IncidentStateException(String message) {
		super(message);
	}

}
