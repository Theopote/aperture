package dev.aperture.fabric.placement;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Converts Minecraft positions to Aperture millimeter vectors and bounds.
 */
public final class McCoordinates {
	private McCoordinates() {
	}

	public static Vec3d toMillimeters(Vec3 position) {
		return new Vec3d(
			McUnits.blocksToMillimeters(position.x),
			McUnits.blocksToMillimeters(position.y),
			McUnits.blocksToMillimeters(position.z)
		);
	}

	public static Vec3d toMillimeters(BlockPos position) {
		return new Vec3d(
			McUnits.blocksToMillimeters(position.getX()),
			McUnits.blocksToMillimeters(position.getY()),
			McUnits.blocksToMillimeters(position.getZ())
		);
	}

	public static BoundingBox blockRegionToBounds(BlockPos minInclusive, BlockPos maxInclusive) {
		Vec3d min = toMillimeters(minInclusive);
		Vec3d max = new Vec3d(
			McUnits.blocksToMillimeters(maxInclusive.getX() + 1),
			McUnits.blocksToMillimeters(maxInclusive.getY() + 1),
			McUnits.blocksToMillimeters(maxInclusive.getZ() + 1)
		);
		return new BoundingBox(min, max);
	}
}
