package dev.aperture.placement.fabric;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Stable host anchor identifiers shared between Fabric adapters and core placement.
 */
public final class HostAnchor {
	private HostAnchor() {
	}

	public static String format(BlockPos origin, Direction face) {
		return origin.getX() + "," + origin.getY() + "," + origin.getZ() + ":" + face.getSerializedName();
	}

	public static String formatRegion(BlockPos minInclusive, BlockPos maxInclusive, Direction face) {
		return minInclusive.getX() + "," + minInclusive.getY() + "," + minInclusive.getZ()
			+ ".." + maxInclusive.getX() + "," + maxInclusive.getY() + "," + maxInclusive.getZ()
			+ ":" + face.getSerializedName();
	}
}
