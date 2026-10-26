package dev.pulsiris.shared;

import java.util.List;

public class ValidationException extends RuntimeException {

	private final List<Violation> violations;

	public ValidationException(List<Violation> violations) {
		super("Validation failed: " + violations);
		this.violations = List.copyOf(violations);
	}

	public List<Violation> violations() {
		return violations;
	}

}
