package dev.aperture.opening.geometry.pipeline.frame;

import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.recipe.shape.ShapeRecipe;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;

/**
 * Opening-specific frame rail layout compiled into kernel {@link ShapeRecipe} ops.
 */
public final class FrameRailBuilder {
	private FrameRailBuilder() {
	}

	public static ShapeRecipe miteredRailShape(
		ProfileCurve profile,
		Vec3d pathStart,
		Vec3d pathEnd,
		Vec3d profileU,
		Vec3d profileV,
		BoundingBox... subtractBoxes
	) {
		return dev.aperture.geometry.recipe.shape.ShapeRecipes.extrudeLinear(
			profile, pathStart, pathEnd, profileU, profileV, subtractBoxes
		);
	}

	public static void emitMiteredRail(
		GeometryCompilationTarget target,
		String componentPath,
		ProfileCurve profile,
		Vec3d pathStart,
		Vec3d pathEnd,
		Vec3d profileU,
		Vec3d profileV,
		BoundingBox... subtractBoxes
	) {
		target.emitSolid(
			componentPath,
			"frame",
			GeometryLayer.OPAQUE,
			miteredRailShape(profile, pathStart, pathEnd, profileU, profileV, subtractBoxes)
		);
	}

	public static BoundingBox corner(dev.aperture.opening.geometry.pipeline.OpeningLayout layout, Corner corner) {
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
		return dev.aperture.geometry.kernel.ProfileExtruder.AXIS_X;
	}

	public static Vec3d axisY() {
		return dev.aperture.geometry.kernel.ProfileExtruder.AXIS_Y;
	}

	public static Vec3d axisZ() {
		return dev.aperture.geometry.kernel.ProfileExtruder.AXIS_Z;
	}

	public enum Corner {
		BOTTOM_LEFT,
		BOTTOM_RIGHT,
		TOP_LEFT,
		TOP_RIGHT
	}
}
