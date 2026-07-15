package dev.aperture.geometry.pipeline.panel;

import dev.aperture.geometry.pipeline.OpeningLayout;
import dev.aperture.geometry.pipeline.OpeningParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes per-panel cell layout from {@code panel_count} and {@code glass_ratio}.
 */
public final class PanelLayoutPlanner {
	private static final double INTER_PANEL_GAP_RATIO = 0.25;

	private PanelLayoutPlanner() {
	}

	public static List<PanelCellLayout> plan(OpeningParameters parameters, OpeningLayout layout) {
		if (!parameters.hasPanel()) {
			return List.of();
		}

		int panelCount = parameters.panelCount();
		double gap = panelCount > 1 ? layout.sashFace() * INTER_PANEL_GAP_RATIO : 0.0;
		double totalGap = gap * (panelCount - 1);
		double cellWidth = (layout.innerWidth() - totalGap) / panelCount;
		double cellHeight = layout.innerHeight();
		if (cellWidth <= 0 || cellHeight <= 0) {
			return List.of();
		}

		double glassHeight = cellHeight * parameters.glassRatio();
		double solidHeight = cellHeight - glassHeight;
		List<PanelCellLayout> cells = new ArrayList<>(panelCount);
		for (int index = 0; index < panelCount; index++) {
			double originX = layout.frameFace() + index * (cellWidth + gap);
			double originY = layout.frameFace();
			boolean operable = isOperableLeaf(index, panelCount, parameters.panelHinge());
			cells.add(new PanelCellLayout(
				index,
				panelCount,
				originX,
				originY,
				cellWidth,
				cellHeight,
				solidHeight,
				glassHeight,
				parameters.panelHinge(),
				operable,
				parameters.openAngleDegrees(),
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
}
