package dev.aperture.geometry.pipeline.glass;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.pipeline.OpeningLayout;
import dev.aperture.geometry.pipeline.OpeningParameters;
import dev.aperture.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.geometry.pipeline.PipelineStep;
import dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder;

/**
 * Generates fixed glazing when no operable panel is present.
 */
public final class GlassGenerator implements PipelineStep {
	public static final String STEP_ID = "glass";
	private static final double GLAZING_DEPTH = 10;

	@Override
	public String id() {
		return STEP_ID;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryAssemblyBuilder assembly) {
		OpeningParameters parameters = context.openingParameters();
		if (parameters.hasPanel()) {
			return;
		}

		OpeningLayout layout = context.layout();
		if (layout.innerWidth() <= 0 || layout.innerHeight() <= 0) {
			return;
		}

		assembly.addSolid(GeometrySolid.box(
			"glazing",
			"glazing",
			GeometryLayer.TRANSLUCENT_GLASS,
			new BoundingBox(
				new Vec3d(layout.frameFace(), layout.frameFace(), 0),
				new Vec3d(layout.frameFace() + layout.innerWidth(), layout.frameFace() + layout.innerHeight(), GLAZING_DEPTH)
			)
		));
	}
}
