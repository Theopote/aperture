package dev.aperture.opening.geometry.pipeline.hardware;

import dev.aperture.core.component.ComponentKind;
import dev.aperture.core.component.HardwareComponent;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.geometry.recipe.shape.ShapeRecipes;
import dev.aperture.opening.geometry.pipeline.ComponentPaths;
import dev.aperture.opening.geometry.pipeline.ComponentPipelineStep;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;
import dev.aperture.opening.geometry.pipeline.panel.PanelCellLayout;
import dev.aperture.opening.geometry.pipeline.panel.PanelLayoutPlanner;
import dev.aperture.opening.resolve.ComponentPropertyResolver;

import java.util.List;

/**
 * Generates hinge plates and mount hardware for one hardware component instance.
 */
public final class HardwareGenerator implements ComponentPipelineStep {
	private static final double HINGE_WIDTH = 18;
	private static final double HINGE_HEIGHT = 90;
	private static final double HINGE_DEPTH = 12;
	private final HardwareComponent component;

	public HardwareGenerator(HardwareComponent component) {
		this.component = component;
	}

	@Override
	public HardwareComponent component() {
		return component;
	}

	@Override
	public void execute(OpeningPipelineContext context, GeometryCompilationTarget target) {
		if (!context.hasComponent(ComponentKind.PANEL)) {
			return;
		}

		OpeningLayout layout = context.layout();
		var generationContext = context.generationContext();
		List<PanelCellLayout> cells = PanelLayoutPlanner.plan(generationContext, layout);
		if (cells.isEmpty()) {
			return;
		}

		String hingeSide = ComponentPropertyResolver.panelHinge(generationContext, "left");
		PanelCellLayout primary = PanelLayoutPlanner.primaryLeaf(cells, hingeSide);
		switch (component.hardwareType()) {
			case "hinge_set", "hinges" -> emitHingeSet(target, primary, layout);
			default -> emitHingeSet(target, primary, layout);
		}
	}

	private void emitHingeSet(
		GeometryCompilationTarget target,
		PanelCellLayout leaf,
		OpeningLayout layout
	) {
		String root = component.ref().id();
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
				ComponentPaths.join(root, "hinge." + (i + 1)),
				"hardware",
				GeometryLayer.CUTOUT,
				ShapeRecipes.box(new BoundingBox(
					new Vec3d(hingeX + offsetX, minY, layout.frameDepth() * 0.15),
					new Vec3d(hingeX + offsetX + HINGE_WIDTH, maxY, layout.frameDepth() * 0.15 + HINGE_DEPTH)
				))
			);
		}
	}
}
