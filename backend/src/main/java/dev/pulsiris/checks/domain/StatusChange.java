package dev.pulsiris.checks.domain;

/**
 * A status transition other modules need to hear about. Most results cause none.
 */
public enum StatusChange {
	WENT_DOWN,
	RECOVERED
}
