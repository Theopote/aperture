package dev.aperture.opening.geometry.pipeline.sill;

import dev.aperture.core.component.SillComponent;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.geometry.recipe.shape.ShapeRecipes;
import dev.aperture.opening.geometry.pipeline.ComponentPaths;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;

/**
 * Generates the sill / threshold for one sill component instance.
 */
public final class SillGenerator implements ComponentPipelineStep {
	private static final double SILL_DEPTH = 20;
	private final SillComponent component;

	public SillGenerator(SillComponent component) {
		this.component = component;
	}

	@Override
	public SillComponent component() {
		return component;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		OpeningLayout layout = context.layout();
		double sillHeight = layout.frameFace() * 0.5;
		if (sillHeight <= 0) {
			return;
		}
		target.emitSolid(
			ComponentPaths.join(component.ref().id(), "main"),
			"frame",
			GeometryLayer.OPAQUE,
			ShapeRecipes.box(new BoundingBox(
				new Vec3d(0, 0, -SILL_DEPTH),
				new Vec3d(layout.width(), sillHeight, 0)
			))
		);
	}
}
