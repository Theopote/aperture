package dev.aperture.fabric.placement;

/**
 * Converts between Minecraft block units and Aperture logical millimeters.
 */
public final class McUnits {
	public static final double MILLIMETERS_PER_BLOCK = 1000.0;

	private McUnits() {
	}

	public static double blocksToMillimeters(int blocks) {
		return blocks * MILLIMETERS_PER_BLOCK;
	}

	public static double blocksToMillimeters(double blocks) {
		return blocks * MILLIMETERS_PER_BLOCK;
	}

	public static int millimetersToBlocks(double millimeters) {
		return (int) Math.floor(millimeters / MILLIMETERS_PER_BLOCK);
	}
}
