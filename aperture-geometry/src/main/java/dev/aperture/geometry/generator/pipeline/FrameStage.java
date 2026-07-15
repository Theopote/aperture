package dev.aperture.geometry.generator.pipeline;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.ops.BooleanOp;
import dev.aperture.geometry.ops.ExtrudeOp;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.profile.ProfileDefinition;
import dev.aperture.geometry.shape.SolidShape;

import java.util.List;

/**
 * Generates frame rails and mullions from catalog profiles with corner miter CSG.
 */
public final class FrameStage implements GenerationStage {
	private static final Vec3d AXIS_X = new Vec3d(1, 0, 0);
	private static final Vec3d AXIS_Y = new Vec3d(0, 1, 0);
	private static final Vec3d AXIS_Z = new Vec3d(0, 0, 1);

	@Override
	public String id() {
		return "frame";
	}

	@Override
	public void contribute(GenerationContext context, GeometryAssemblyBuilder builder) {
		double width = context.requireLength("width");
		double height = context.requireLength("height");
		int mullions = context.requireCount("mullions");

		ProfileDefinition profileDefinition = context.scaledFrameProfile();
		ProfileCurve profile = profileDefinition.curve();
		double frameFace = profileDefinition.bounds().width();
		double frameDepth = profileDefinition.bounds().depth();

		double innerWidth = width - frameFace * 2;
		double innerHeight = height - frameFace * 2;

		builder.addSolid(miteredRail(
			"frame.bottom",
			profile,
			frameDepth,
			new Vec3d(0, 0, 0),
			new Vec3d(width, 0, 0),
			AXIS_Y,
			AXIS_Z,
			corner(width, height, frameFace, frameDepth, Corner.BOTTOM_LEFT),
			corner(width, height, frameFace, frameDepth, Corner.BOTTOM_RIGHT)
		));
		builder.addSolid(miteredRail(
			"frame.top",
			profile,
			frameDepth,
			new Vec3d(0, height - frameFace, 0),
			new Vec3d(width, height - frameFace, 0),
			AXIS_Y,
			AXIS_Z,
			corner(width, height, frameFace, frameDepth, Corner.TOP_LEFT),
			corner(width, height, frameFace, frameDepth, Corner.TOP_RIGHT)
		));
		builder.addSolid(miteredRail(
			"frame.left",
			profile,
			frameDepth,
			new Vec3d(0, frameFace, 0),
			new Vec3d(0, height - frameFace, 0),
			AXIS_X,
			AXIS_Z,
			corner(width, height, frameFace, frameDepth, Corner.BOTTOM_LEFT),
			corner(width, height, frameFace, frameDepth, Corner.TOP_LEFT)
		));
		builder.addSolid(miteredRail(
			"frame.right",
			profile,
			frameDepth,
			new Vec3d(width - frameFace, frameFace, 0),
			new Vec3d(width - frameFace, height - frameFace, 0),
			AXIS_X,
			AXIS_Z,
			corner(width, height, frameFace, frameDepth, Corner.BOTTOM_RIGHT),
			corner(width, height, frameFace, frameDepth, Corner.TOP_RIGHT)
		));

		for (int i = 1; i <= mullions; i++) {
			double t = (double) i / (mullions + 1);
			double x = frameFace + innerWidth * t - frameFace / 2.0;
			builder.addSolid(miteredRail(
				"frame.mullion." + i,
				profile,
				frameDepth,
				new Vec3d(x, frameFace, 0),
				new Vec3d(x, height - frameFace, 0),
				AXIS_X,
				AXIS_Z
			));
		}

		builder.setCutVolume(BoundingBox.fromSize(width, height, 200));
	}

	private static GeometrySolid miteredRail(
		String componentPath,
		ProfileCurve profile,
		double frameDepth,
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

	private static BoundingBox corner(double width, double height, double frameFace, double frameDepth, Corner corner) {
		return switch (corner) {
			case BOTTOM_LEFT -> new BoundingBox(Vec3d.ZERO, new Vec3d(frameFace, frameFace, frameDepth));
			case BOTTOM_RIGHT -> new BoundingBox(
				new Vec3d(width - frameFace, 0, 0),
				new Vec3d(width, frameFace, frameDepth)
			);
			case TOP_LEFT -> new BoundingBox(
				new Vec3d(0, height - frameFace, 0),
				new Vec3d(frameFace, height, frameDepth)
			);
			case TOP_RIGHT -> new BoundingBox(
				new Vec3d(width - frameFace, height - frameFace, 0),
				new Vec3d(width, height, frameDepth)
			);
		};
	}

	private enum Corner {
		BOTTOM_LEFT,
		BOTTOM_RIGHT,
		TOP_LEFT,
		TOP_RIGHT
	}
}
