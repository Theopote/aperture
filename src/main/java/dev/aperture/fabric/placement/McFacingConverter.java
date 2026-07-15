package dev.aperture.fabric.placement;

import dev.aperture.core.geometry.Facing;
import net.minecraft.core.Direction;

/**
 * Converts between Minecraft {@link Direction} and Aperture {@link Facing}.
 */
public final class McFacingConverter {
	private McFacingConverter() {
	}

	public static Facing toFacing(Direction direction) {
		return switch (direction) {
			case NORTH -> Facing.NORTH;
			case SOUTH -> Facing.SOUTH;
			case EAST -> Facing.EAST;
			case WEST -> Facing.WEST;
			case UP -> Facing.UP;
			case DOWN -> Facing.DOWN;
		};
	}

	public static Direction toDirection(Facing facing) {
		return switch (facing) {
			case NORTH -> Direction.NORTH;
			case SOUTH -> Direction.SOUTH;
			case EAST -> Direction.EAST;
			case WEST -> Direction.WEST;
			case UP -> Direction.UP;
			case DOWN -> Direction.DOWN;
		};
	}
}
