package dev.aperture.geometry.ops;

import dev.aperture.math.Vec3d;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.shape.ExtrusionShape;

/**
 * Geometry operations for building solid shapes.
 */
public final class ExtrudeOp {
	private ExtrudeOp() {
	}

	public static ExtrusionShape linear(
		ProfileCurve profile,
		Vec3d pathStart,
		Vec3d pathEnd,
		Vec3d profileU,
		Vec3d profileV
	) {
		return new ExtrusionShape(profile, pathStart, pathEnd, profileU, profileV);
	}
}
