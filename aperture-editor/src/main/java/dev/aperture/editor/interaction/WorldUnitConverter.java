package dev.aperture.editor.interaction;

import dev.aperture.math.Vec3d;

/** Converts only at the adapter boundary between Minecraft blocks and architectural millimetres. */
public final class WorldUnitConverter {
	public static final double MILLIMETERS_PER_BLOCK = 1000.0;

	private WorldUnitConverter() { }

	public static double blocksToLength(double blocks) {
		return blocks * MILLIMETERS_PER_BLOCK;
	}

	public static double lengthToBlocks(double millimeters) {
		return millimeters / MILLIMETERS_PER_BLOCK;
	}

	public static Vec3d blocksToWorldLength(Vec3d blocks) {
		return blocks.scale(MILLIMETERS_PER_BLOCK);
	}

	public static Vec3d worldLengthToBlocks(Vec3d millimeters) {
		return millimeters.scale(1.0 / MILLIMETERS_PER_BLOCK);
	}
}
