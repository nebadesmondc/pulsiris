package dev.pulsiris.shared;

public abstract class ConflictException extends RuntimeException {

	protected ConflictException(String message) {
		super(message);
	}

}
