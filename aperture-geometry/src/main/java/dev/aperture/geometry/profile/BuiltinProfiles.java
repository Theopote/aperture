package dev.aperture.geometry.profile;

/**
 * Built-in frame profiles for Phase 0 geometry development.
 */
public final class BuiltinProfiles {
	public static ProfileCurve frameRect(double width, double depth) {
		return ProfileCurve.rectangle(0, 0, width, depth);
	}

	private BuiltinProfiles() {
	}
}
