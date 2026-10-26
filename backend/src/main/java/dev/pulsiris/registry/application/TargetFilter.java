package dev.pulsiris.registry.application;

/**
 * Optional criteria for listing targets. A {@code null} field means "don't filter on it".
 *
 * @param name case-insensitive part of the target name
 * @param enabled only enabled or only disabled targets
 */
public record TargetFilter(String name, Boolean enabled) {

	public static TargetFilter none() {
		return new TargetFilter(null, null);
	}

}
