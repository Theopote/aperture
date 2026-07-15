package dev.aperture.opening.geometry.pipeline.handle;

import dev.aperture.core.component.HandleComponent;
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
import dev.aperture.opening.geometry.pipeline.panel.PanelCellLayout;
import dev.aperture.opening.geometry.pipeline.panel.PanelLayoutPlanner;

import java.util.List;

/**
 * Generates latch hardware for one handle component instance.
 */
public final class HandleGenerator implements ComponentPipelineStep {
	private static final double HANDLE_WIDTH = 24;
	private static final double HANDLE_HEIGHT = 120;
	private static final double HANDLE_DEPTH = 16;
	private final HandleComponent component;

	public HandleGenerator(HandleComponent component) {
		this.component = component;
	}

	@Override
	public HandleComponent component() {
		return component;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		OpeningParameters parameters = context.openingParameters();
		if (!parameters.hasPanel()) {
			return;
		}

		OpeningLayout layout = context.layout();
		List<PanelCellLayout> cells = PanelLayoutPlanner.plan(parameters, layout);
		if (cells.isEmpty()) {
			return;
		}

		PanelCellLayout primary = PanelLayoutPlanner.primaryLeaf(cells, parameters.panelHinge());
		emitHandle(target, primary, layout);
	}

	private void emitHandle(
		GeometryCompilationTarget target,
		PanelCellLayout leaf,
		OpeningLayout layout
	) {
		String root = component.ref().id();
		double latchX = leaf.latchX();
		double centerY = leaf.originY() + leaf.height() * 0.48;
		double offsetX = switch (leaf.hingeSide().toLowerCase()) {
			case "right" -> layout.sashFace() * 0.2;
			default -> -HANDLE_WIDTH - layout.sashFace() * 0.2;
		};
		target.emitSolid(
			ComponentPaths.join(root, "main"),
			"hardware",
			GeometryLayer.CUTOUT,
			ShapeRecipes.box(new BoundingBox(
				new Vec3d(latchX + offsetX, centerY - HANDLE_HEIGHT / 2.0, layout.frameDepth() * 0.35),
				new Vec3d(latchX + offsetX + HANDLE_WIDTH, centerY + HANDLE_HEIGHT / 2.0, layout.frameDepth() * 0.35 + HANDLE_DEPTH)
			))
		);
	}
}
