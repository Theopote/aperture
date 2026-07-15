package dev.aperture.opening.geometry.pipeline.glass;

import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningParameters;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.opening.geometry.pipeline.PipelineStep;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;

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
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		OpeningParameters parameters = context.openingParameters();
		if (parameters.hasPanel()) {
			return;
		}

		OpeningLayout layout = context.layout();
		if (layout.innerWidth() <= 0 || layout.innerHeight() <= 0) {
			return;
		}

		if (parameters.cols() > 1 || parameters.rows() > 1) {
			emitGridGlazing(target, layout, parameters);
			return;
		}

		target.addSolid(GeometrySolid.box(
			"glazing",
			"glazing",
			GeometryLayer.TRANSLUCENT,
			new BoundingBox(
				new Vec3d(layout.frameFace(), layout.frameFace(), 0),
				new Vec3d(layout.frameFace() + layout.innerWidth(), layout.frameFace() + layout.innerHeight(), GLAZING_DEPTH)
			)
		));
	}

	private static void emitGridGlazing(
		GeometryCompilationTarget target,
		OpeningLayout layout,
		OpeningParameters parameters
	) {
		double cellWidth = layout.innerWidth() / parameters.cols();
		double cellHeight = layout.innerHeight() / parameters.rows();
		for (int row = 0; row < parameters.rows(); row++) {
			for (int col = 0; col < parameters.cols(); col++) {
				double minX = layout.frameFace() + col * cellWidth + layout.frameFace() * 0.25;
				double maxX = layout.frameFace() + (col + 1) * cellWidth - layout.frameFace() * 0.25;
				double minY = layout.frameFace() + row * cellHeight + layout.frameFace() * 0.25;
				double maxY = layout.frameFace() + (row + 1) * cellHeight - layout.frameFace() * 0.25;
				if (maxX <= minX || maxY <= minY) {
					continue;
				}
				target.addSolid(GeometrySolid.box(
					"glazing." + row + "." + col,
					"glazing",
					GeometryLayer.TRANSLUCENT,
					new BoundingBox(
						new Vec3d(minX, minY, 0),
						new Vec3d(maxX, maxY, GLAZING_DEPTH)
					)
				));
			}
		}
	}
}
