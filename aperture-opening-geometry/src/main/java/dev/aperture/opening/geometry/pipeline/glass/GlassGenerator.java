package dev.aperture.opening.geometry.pipeline.glass;

import dev.aperture.core.component.GlassComponent;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.geometry.recipe.shape.ShapeRecipes;
import dev.aperture.opening.geometry.pipeline.ComponentPaths;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningParameters;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;

/**
 * Generates fixed glazing for one glass component instance when no operable panel is present.
 */
public final class GlassGenerator implements ComponentPipelineStep {
	private static final double GLAZING_DEPTH = 10;
	private final GlassComponent component;

	public GlassGenerator(GlassComponent component) {
		this.component = component;
	}

	@Override
	public GlassComponent component() {
		return component;
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

		String root = component.ref().id();
		if (parameters.cols() > 1 || parameters.rows() > 1) {
			emitGridGlazing(target, layout, parameters, root);
			return;
		}

		target.emitSolid(
			root,
			"glazing",
			GeometryLayer.TRANSLUCENT,
			ShapeRecipes.box(new BoundingBox(
				new Vec3d(layout.frameFace(), layout.frameFace(), 0),
				new Vec3d(layout.frameFace() + layout.innerWidth(), layout.frameFace() + layout.innerHeight(), GLAZING_DEPTH)
			))
		);
	}

	private static void emitGridGlazing(
		GeometryCompilationTarget target,
		OpeningLayout layout,
		OpeningParameters parameters,
		String root
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
				target.emitSolid(
					ComponentPaths.join(root, row + "." + col),
					"glazing",
					GeometryLayer.TRANSLUCENT,
					ShapeRecipes.box(new BoundingBox(
						new Vec3d(minX, minY, 0),
						new Vec3d(maxX, maxY, GLAZING_DEPTH)
					))
				);
			}
		}
	}
}
