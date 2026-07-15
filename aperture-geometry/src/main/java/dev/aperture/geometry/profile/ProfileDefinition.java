package dev.aperture.geometry.profile;

/**
 * Immutable profile definition loaded from catalog data.
 */
public record ProfileDefinition(
	String id,
	String name,
	ProfileCurve curve
) {
	public ProfileBounds bounds() {
		return curve.bounds();
	}
}
