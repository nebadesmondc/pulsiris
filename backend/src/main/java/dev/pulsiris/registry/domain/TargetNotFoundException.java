package dev.pulsiris.registry.domain;

import java.util.UUID;

import dev.pulsiris.shared.NotFoundException;

public class TargetNotFoundException extends NotFoundException {

	public TargetNotFoundException(UUID id) {
		super("Target " + id + " not found");
	}

}
