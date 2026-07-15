package dev.aperture.core.geometry;

/**
 * Cardinal facing used for placement and host alignment.
 */
public enum Facing {
	NORTH(0, 0, -1),
	SOUTH(0, 0, 1),
	EAST(1, 0, 0),
	WEST(-1, 0, 0),
	UP(0, 1, 0),
	DOWN(0, -1, 0);

	private final int x;
	private final int y;
	private final int z;

	Facing(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int z() {
		return z;
	}

	public static Facing fromId(String id) {
		return valueOf(id.toUpperCase());
	}

	public Facing rotateClockwise() {
		return switch (this) {
			case NORTH -> EAST;
			case EAST -> SOUTH;
			case SOUTH -> WEST;
			case WEST -> NORTH;
			default -> this;
		};
	}

	public Facing getOpposite() {
		return switch (this) {
			case NORTH -> SOUTH;
			case SOUTH -> NORTH;
			case EAST -> WEST;
			case WEST -> EAST;
			case UP -> DOWN;
			case DOWN -> UP;
		};
	}
}
