package dev.aperture.geometry.kernel;

import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.ops.BooleanOp;
import dev.aperture.geometry.ops.ExtrudeOp;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.shape.SolidShape;

/**
 * Pure profile extrusion with optional boolean corner cuts — no opening-domain knowledge.
 */
public final class ProfileExtruder {
	public static final Vec3d AXIS_X = new Vec3d(1, 0, 0);
	public static final Vec3d AXIS_Y = new Vec3d(0, 1, 0);
	public static final Vec3d AXIS_Z = new Vec3d(0, 0, 1);

	private ProfileExtruder() {
	}

	public static GeometrySolid linear(
		String partPath,
		String materialGroup,
		GeometryLayer layer,
		ProfileCurve profile,
		Vec3d pathStart,
		Vec3d pathEnd,
		Vec3d profileU,
		Vec3d profileV,
		BoundingBox... subtractBoxes
	) {
		SolidShape shape = ExtrudeOp.linear(profile, pathStart, pathEnd, profileU, profileV);
		if (subtractBoxes.length > 0) {
			shape = BooleanOp.subtractBoxes(shape, subtractBoxes);
		}
		return GeometrySolid.of(partPath, materialGroup, layer, shape);
	}
}
