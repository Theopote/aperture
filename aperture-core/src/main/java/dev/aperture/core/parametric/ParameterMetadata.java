package dev.aperture.core.parametric;

/**
 * UI and tooling metadata for a parameter. Does not affect geometry directly.
 */
public record ParameterMetadata(
	String label,
	String group,
	String description,
	boolean readOnly
) {
	public static ParameterMetadata defaults() {
		return new ParameterMetadata("", "", "", false);
	}

	public static ParameterMetadata labeled(String label) {
		return new ParameterMetadata(label, "", "", false);
	}

	public static ParameterMetadata grouped(String label, String group) {
		return new ParameterMetadata(label, group, "", false);
	}
}
