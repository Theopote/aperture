package dev.aperture.geometry.generators;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.api.generator.OpeningGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 0 reference generator: rectangular frame + glazing opening.
 * Produces bounding-box solids; mesh compilation is deferred to the render adapter.
 */
public final class RectangularWindowGenerator implements OpeningGenerator {
	public static final String ID = "aperture:rectangular_window_v1";

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

		List<GeometrySolid> solids = new ArrayList<>();

		solids.add(new GeometrySolid(
			"frame",
			"frame",
			GeometryLayer.OPAQUE_FRAME,
			dev.aperture.core.geometry.BoundingBox.fromSize(width, height, frameWidth)
		));

		double innerWidth = width - frameWidth * 2;
		double innerHeight = height - frameWidth * 2;
		if (innerWidth > 0 && innerHeight > 0) {
			solids.add(new GeometrySolid(
				"glazing",
				"glazing",
				GeometryLayer.TRANSLUCENT_GLASS,
				new dev.aperture.core.geometry.BoundingBox(
					new dev.aperture.core.geometry.Vec3d(frameWidth, frameWidth, 0),
					new dev.aperture.core.geometry.Vec3d(frameWidth + innerWidth, frameWidth + innerHeight, 10)
				)
			));
		}

		for (int i = 1; i <= mullions; i++) {
			double t = (double) i / (mullions + 1);
			double x = frameWidth + innerWidth * t;
			solids.add(new GeometrySolid(
				"frame.mullion." + i,
				"frame",
				GeometryLayer.OPAQUE_FRAME,
				new dev.aperture.core.geometry.BoundingBox(
					new dev.aperture.core.geometry.Vec3d(x - frameWidth / 2.0, frameWidth, 0),
					new dev.aperture.core.geometry.Vec3d(x + frameWidth / 2.0, frameWidth + innerHeight, frameWidth)
				)
			));
		}

		var bounds = dev.aperture.core.geometry.BoundingBox.fromSize(width, height, frameWidth);
		var cutVolume = dev.aperture.core.geometry.BoundingBox.fromSize(width, height, 200);

		return new GeometryResult(solids, bounds, cutVolume);
	}
}
