package dev.pulsiris.registry.domain;

import dev.pulsiris.shared.ConflictException;

public class DuplicateTargetNameException extends ConflictException {

	public DuplicateTargetNameException(String name) {
		super("A target named '" + name + "' already exists");
	}

}
