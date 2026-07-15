package dev.aperture.fabric.placement;

import dev.aperture.core.geometry.BoundingBox;
import net.minecraft.world.phys.AABB;

/**
 * Converts Aperture millimeter bounds to Minecraft {@link AABB} (block units).
 */
public final class McBoundsConverter {
	private McBoundsConverter() {
	}

	public static AABB toAabb(BoundingBox bounds) {
		return new AABB(
			toBlocks(bounds.min().x()),
			toBlocks(bounds.min().y()),
			toBlocks(bounds.min().z()),
			toBlocks(bounds.max().x()),
			toBlocks(bounds.max().y()),
			toBlocks(bounds.max().z())
		);
	}

	private static double toBlocks(double millimeters) {
		return millimeters / McUnits.MILLIMETERS_PER_BLOCK;
	}
}
