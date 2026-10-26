package dev.pulsiris.checks.application;

import java.util.UUID;

import dev.pulsiris.shared.NotFoundException;

public class UnknownTargetException extends NotFoundException {

	public UnknownTargetException(UUID id) {
		super("Target " + id + " not found");
	}

}
