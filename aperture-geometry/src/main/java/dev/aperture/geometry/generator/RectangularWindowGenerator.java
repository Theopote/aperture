package dev.aperture.geometry.generator;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.ops.ExtrudeOp;
import dev.aperture.geometry.profile.BuiltinProfiles;
import dev.aperture.geometry.profile.ProfileCurve;

import java.util.ArrayList;
import java.util.List;

/**
 * Reference generator: rectangular frame rails extruded from a profile plus glazing panel.
 */
public final class RectangularWindowGenerator implements OpeningGenerator {
	public static final String ID = "aperture:rectangular_window_v1";

	private static final Vec3d AXIS_X = new Vec3d(1, 0, 0);
	private static final Vec3d AXIS_Y = new Vec3d(0, 1, 0);
	private static final Vec3d AXIS_Z = new Vec3d(0, 0, 1);

	@Override
	public String id() {
		return ID;
	}

	@Override
	public GeometryResult generate(OpeningTypeDefinition definition, ParameterSet parameters) {
		double width = parameters.requireLength("width");
		double height = parameters.requireLength("height");
		double frameWidth = parameters.requireLength("frame_width");
		int mullions = parameters.requireCount("mullions");

		ProfileCurve profile = BuiltinProfiles.frameRect(frameWidth, frameWidth);
		List<GeometrySolid> solids = new ArrayList<>();

		solids.add(rail("frame.bottom", width, profile,
			new Vec3d(0, 0, 0),
			new Vec3d(width, 0, 0),
			AXIS_Y,
			AXIS_Z
		));
		solids.add(rail("frame.top", width, profile,
			new Vec3d(0, height - frameWidth, 0),
			new Vec3d(width, height - frameWidth, 0),
			AXIS_Y,
			AXIS_Z
		));
		solids.add(rail("frame.left", height - frameWidth * 2, profile,
			new Vec3d(0, frameWidth, 0),
			new Vec3d(0, height - frameWidth, 0),
			AXIS_X,
			AXIS_Z
		));
		solids.add(rail("frame.right", height - frameWidth * 2, profile,
			new Vec3d(width - frameWidth, frameWidth, 0),
			new Vec3d(width - frameWidth, height - frameWidth, 0),
			AXIS_X,
			AXIS_Z
		));

		double innerWidth = width - frameWidth * 2;
		double innerHeight = height - frameWidth * 2;
		if (innerWidth > 0 && innerHeight > 0) {
			solids.add(GeometrySolid.box(
				"glazing",
				"glazing",
				GeometryLayer.TRANSLUCENT_GLASS,
				new BoundingBox(
					new Vec3d(frameWidth, frameWidth, 0),
					new Vec3d(frameWidth + innerWidth, frameWidth + innerHeight, 10)
				)
			));
		}

		for (int i = 1; i <= mullions; i++) {
			double t = (double) i / (mullions + 1);
			double x = frameWidth + innerWidth * t - frameWidth / 2.0;
			solids.add(rail("frame.mullion." + i, innerHeight, profile,
				new Vec3d(x, frameWidth, 0),
				new Vec3d(x, height - frameWidth, 0),
				AXIS_X,
				AXIS_Z
			));
		}

		var bounds = BoundingBox.fromSize(width, height, frameWidth);
		var cutVolume = BoundingBox.fromSize(width, height, 200);

		return new GeometryResult(solids, bounds, cutVolume);
	}

	private static GeometrySolid rail(
		String componentPath,
		double span,
		ProfileCurve profile,
		Vec3d pathStart,
		Vec3d pathEnd,
		Vec3d profileU,
		Vec3d profileV
	) {
		if (span <= 0) {
			throw new IllegalArgumentException("rail span must be positive: " + componentPath);
		}
		return GeometrySolid.of(
			componentPath,
			"frame",
			GeometryLayer.OPAQUE_FRAME,
			ExtrudeOp.linear(profile, pathStart, pathEnd, profileU, profileV)
		);
	}
}
