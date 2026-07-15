package dev.aperture.opening.geometry.pipeline.glass;

import dev.aperture.core.component.ComponentKind;
import dev.aperture.core.component.GlassComponent;
import dev.aperture.core.parameter.ParameterSet;
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
		if (context.hasComponent(ComponentKind.PANEL)) {
			return;
		}

		OpeningLayout layout = context.layout();
		if (layout.innerWidth() <= 0 || layout.innerHeight() <= 0) {
			return;
		}

		ParameterSet parameters = context.parameters();
		int cols = Math.max(1, parameters.countOrDefault("cols", 1));
		int rows = Math.max(1, parameters.countOrDefault("rows", 1));
		String root = component.ref().id();
		if (cols > 1 || rows > 1) {
			emitGridGlazing(target, layout, cols, rows, root);
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
		int cols,
		int rows,
		String root
	) {
		double cellWidth = layout.innerWidth() / cols;
		double cellHeight = layout.innerHeight() / rows;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
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
