package dev.aperture.geometry.pipeline.frame;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.ops.BooleanOp;
import dev.aperture.geometry.ops.ExtrudeOp;
import dev.aperture.geometry.pipeline.OpeningLayout;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.shape.SolidShape;

/**
 * Shared frame rail extrusion with optional corner miter CSG.
 */
public final class FrameRailBuilder {
	private static final Vec3d AXIS_X = new Vec3d(1, 0, 0);
	private static final Vec3d AXIS_Y = new Vec3d(0, 1, 0);
	private static final Vec3d AXIS_Z = new Vec3d(0, 0, 1);

	private FrameRailBuilder() {
	}

	public static GeometrySolid miteredRail(
		String componentPath,
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
		return GeometrySolid.of(componentPath, "frame", GeometryLayer.OPAQUE_FRAME, shape);
	}

	public static BoundingBox corner(OpeningLayout layout, Corner corner) {
		return switch (corner) {
			case BOTTOM_LEFT -> new BoundingBox(Vec3d.ZERO, new Vec3d(layout.frameFace(), layout.frameFace(), layout.frameDepth()));
			case BOTTOM_RIGHT -> new BoundingBox(
				new Vec3d(layout.width() - layout.frameFace(), 0, 0),
				new Vec3d(layout.width(), layout.frameFace(), layout.frameDepth())
			);
			case TOP_LEFT -> new BoundingBox(
				new Vec3d(0, layout.height() - layout.frameFace(), 0),
				new Vec3d(layout.frameFace(), layout.height(), layout.frameDepth())
			);
			case TOP_RIGHT -> new BoundingBox(
				new Vec3d(layout.width() - layout.frameFace(), layout.height() - layout.frameFace(), 0),
				new Vec3d(layout.width(), layout.height(), layout.frameDepth())
			);
		};
	}

	public static Vec3d axisX() {
		return AXIS_X;
	}

	public static Vec3d axisY() {
		return AXIS_Y;
	}

	public static Vec3d axisZ() {
		return AXIS_Z;
	}

	public enum Corner {
		BOTTOM_LEFT,
		BOTTOM_RIGHT,
		TOP_LEFT,
		TOP_RIGHT
	}
}
