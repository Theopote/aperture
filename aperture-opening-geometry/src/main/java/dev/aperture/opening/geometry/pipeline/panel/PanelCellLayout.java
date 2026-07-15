package dev.aperture.opening.geometry.pipeline.panel;

import dev.aperture.opening.geometry.pipeline.OpeningLayout;

/**
 * One panel leaf within the opening inner frame, including glazed and solid regions.
 */
public record PanelCellLayout(
	int index,
	int panelCount,
	double originX,
	double originY,
	double width,
	double height,
	double solidHeight,
	double glassHeight,
	String hingeSide,
	boolean operable,
	double openAngleDegrees,
	OpeningLayout layout
) {
	public boolean usesLegacyPaths() {
		return panelCount == 1;
	}

	public String pathPrefix() {
		return usesLegacyPaths() ? "panel" : "panel." + index;
	}

	public double glassBottomY() {
		return originY + solidHeight;
	}

	public double hingeX() {
		return switch (hingeSide.toLowerCase()) {
			case "right" -> originX + width - layout.sashFace();
			default -> originX;
		};
	}

	public double latchX() {
		return switch (hingeSide.toLowerCase()) {
			case "right" -> originX + layout.sashFace();
			default -> originX + width - layout.sashFace();
		};
	}
}
