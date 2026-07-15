package dev.aperture.geometry.pipeline.sill;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.pipeline.OpeningLayout;
import dev.aperture.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.geometry.pipeline.PipelineStep;
import dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder;

/**
 * Generates the sill / threshold at the bottom of the opening.
 */
public final class SillGenerator implements PipelineStep {
	public static final String STEP_ID = "sill";
	private static final double SILL_DEPTH = 20;

	@Override
	public String id() {
		return STEP_ID;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryAssemblyBuilder assembly) {
		OpeningLayout layout = context.layout();
		double sillHeight = layout.frameFace() * 0.5;
		if (sillHeight <= 0) {
			return;
		}
		assembly.addSolid(GeometrySolid.box(
			"sill.main",
			"frame",
			GeometryLayer.OPAQUE_FRAME,
			new BoundingBox(
				new Vec3d(0, 0, -SILL_DEPTH),
				new Vec3d(layout.width(), sillHeight, 0)
			)
		));
	}
}
