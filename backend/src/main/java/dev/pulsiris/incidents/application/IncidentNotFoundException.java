package dev.pulsiris.incidents.application;

import java.util.UUID;

import dev.pulsiris.shared.NotFoundException;

public class IncidentNotFoundException extends NotFoundException {

	public IncidentNotFoundException(UUID id) {
		super("Incident " + id + " not found");
	}

}
