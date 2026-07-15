package dev.aperture.opening.geometry.pipeline.hardware;

import dev.aperture.core.component.ComponentKind;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.geometry.recipe.shape.ShapeRecipes;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningParameters;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.opening.geometry.pipeline.PipelineStep;
import dev.aperture.opening.geometry.pipeline.panel.PanelCellLayout;
import dev.aperture.opening.geometry.pipeline.panel.PanelLayoutPlanner;

import java.util.List;

/**
 * Generates hinge plates, handles, and other mount hardware geometry.
 */
public final class HardwareGenerator implements PipelineStep {
	public static final String STEP_ID = "hardware";
	private static final double HINGE_WIDTH = 18;
	private static final double HINGE_HEIGHT = 90;
	private static final double HINGE_DEPTH = 12;
	private static final double HANDLE_WIDTH = 24;
	private static final double HANDLE_HEIGHT = 120;
	private static final double HANDLE_DEPTH = 16;

	@Override
	public String id() {
		return STEP_ID;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		if (!context.definition().components().has(ComponentKind.HARDWARE)) {
			return;
		}

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
		emitHingeSet(target, primary, layout);
		emitHandle(target, primary, layout);
	}

	private static void emitHingeSet(
		GeometryCompilationTarget target,
		PanelCellLayout leaf,
		OpeningLayout layout
	) {
		double hingeX = leaf.hingeX();
		double offsetX = switch (leaf.hingeSide().toLowerCase()) {
			case "right" -> -HINGE_DEPTH;
			default -> 0;
		};
		double[] positions = {0.2, 0.5, 0.8};
		for (int i = 0; i < positions.length; i++) {
			double centerY = leaf.originY() + leaf.height() * positions[i];
			double minY = centerY - HINGE_HEIGHT / 2.0;
			double maxY = centerY + HINGE_HEIGHT / 2.0;
			target.emitSolid(
				"hardware.hinge." + (i + 1),
				"hardware",
				GeometryLayer.CUTOUT,
				ShapeRecipes.box(new BoundingBox(
					new Vec3d(hingeX + offsetX, minY, layout.frameDepth() * 0.15),
					new Vec3d(hingeX + offsetX + HINGE_WIDTH, maxY, layout.frameDepth() * 0.15 + HINGE_DEPTH)
				))
			);
		}
	}

	private static void emitHandle(
		GeometryCompilationTarget target,
		PanelCellLayout leaf,
		OpeningLayout layout
	) {
		double latchX = leaf.latchX();
		double centerY = leaf.originY() + leaf.height() * 0.48;
		double offsetX = switch (leaf.hingeSide().toLowerCase()) {
			case "right" -> layout.sashFace() * 0.2;
			default -> -HANDLE_WIDTH - layout.sashFace() * 0.2;
		};
		target.emitSolid(
			"hardware.handle",
			"hardware",
			GeometryLayer.CUTOUT,
			ShapeRecipes.box(new BoundingBox(
				new Vec3d(latchX + offsetX, centerY - HANDLE_HEIGHT / 2.0, layout.frameDepth() * 0.35),
				new Vec3d(latchX + offsetX + HANDLE_WIDTH, centerY + HANDLE_HEIGHT / 2.0, layout.frameDepth() * 0.35 + HANDLE_DEPTH)
			))
		);
	}
}
