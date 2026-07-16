package dev.aperture.opening.geometry.pipeline.panel;

import dev.aperture.core.component.ComponentKind;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.resolve.ComponentPropertyResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes per-panel cell layout from {@code panel_count} and {@code glass_ratio} parameters.
 */
public final class PanelLayoutPlanner {
	private static final double INTER_PANEL_GAP_RATIO = 0.25;

	private PanelLayoutPlanner() {
	}

	public static List<PanelCellLayout> plan(GenerationContext context, OpeningLayout layout) {
		ParameterSet parameters = context.parameters();
		String hingeSide = ComponentPropertyResolver.panelHinge(context, "left");
		return plan(context, layout, hingeSide);
	}

	public static List<PanelCellLayout> plan(
		GenerationContext context,
		OpeningLayout layout,
		String hingeSide
	) {
		if (!context.hasComponent(ComponentKind.PANEL)) {
			return List.of();
		}

		ParameterSet parameters = context.parameters();
		int panelCount = Math.max(1, parameters.countOrDefault("panel_count", 1));
		double glassRatio = clampRatio(parameters.numberOrDefault("glass_ratio", 1.0));
		double openAngleDegrees = parameters.angleOrDefault("open_angle", 0.0);

		double gap = panelCount > 1 ? layout.sashFace() * INTER_PANEL_GAP_RATIO : 0.0;
		double totalGap = gap * (panelCount - 1);
		double cellWidth = (layout.innerWidth() - totalGap) / panelCount;
		double cellHeight = layout.innerHeight();
		if (cellWidth <= 0 || cellHeight <= 0) {
			return List.of();
		}

		double glassHeight = cellHeight * glassRatio;
		double solidHeight = cellHeight - glassHeight;
		List<PanelCellLayout> cells = new ArrayList<>(panelCount);
		for (int index = 0; index < panelCount; index++) {
			double originX = layout.frameFace() + index * (cellWidth + gap);
			double originY = layout.frameFace();
			boolean operable = isOperableLeaf(index, panelCount, hingeSide);
			cells.add(new PanelCellLayout(
				index,
				panelCount,
				originX,
				originY,
				cellWidth,
				cellHeight,
				solidHeight,
				glassHeight,
				hingeSide,
				operable,
				openAngleDegrees,
				layout
			));
		}
		return cells;
	}

	public static PanelCellLayout primaryLeaf(List<PanelCellLayout> cells, String hingeSide) {
		if (cells.isEmpty()) {
			throw new IllegalStateException("No panel cells to resolve primary leaf");
		}
		return switch (hingeSide.toLowerCase()) {
			case "right" -> cells.getLast();
			default -> cells.getFirst();
		};
	}

	private static boolean isOperableLeaf(int index, int panelCount, String hingeSide) {
		if (panelCount == 1) {
			return true;
		}
		return switch (hingeSide.toLowerCase()) {
			case "right" -> index == panelCount - 1;
			default -> index == 0;
		};
	}

	private static double clampRatio(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}
}
