package dev.aperture.geometry.generator.pipeline;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.profile.ProfileDefinition;

/**
 * Generates the glazing panel within the frame opening.
 */
public final class GlassStage implements GenerationStage {
	private static final double GLAZING_DEPTH = 10;

	@Override
	public String id() {
		return "glass";
	}

	@Override
	public void contribute(GenerationContext context, GeometryAssemblyBuilder builder) {
		if (context.hasComponent("panel")) {
			return;
		}

		double width = context.requireLength("width");
		double height = context.requireLength("height");

		ProfileDefinition profileDefinition = context.scaledFrameProfile();
		double frameFace = profileDefinition.bounds().width();

		double innerWidth = width - frameFace * 2;
		double innerHeight = height - frameFace * 2;
		if (innerWidth <= 0 || innerHeight <= 0) {
			return;
		}

		builder.addSolid(GeometrySolid.box(
			"glazing",
			"glazing",
			GeometryLayer.TRANSLUCENT_GLASS,
			new BoundingBox(
				new Vec3d(frameFace, frameFace, 0),
				new Vec3d(frameFace + innerWidth, frameFace + innerHeight, GLAZING_DEPTH)
			)
		));
	}
}
