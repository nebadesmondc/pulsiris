package dev.pulsiris.incidents.domain;

/**
 * Why an incident was resolved.
 */
public enum Resolution {

	/** The target passed its recovery checks. */
	RECOVERED,

	/** A person resolved it; the target may still have been down. */
	MANUAL,

	/** The target was removed from monitoring. */
	TARGET_REMOVED

}
