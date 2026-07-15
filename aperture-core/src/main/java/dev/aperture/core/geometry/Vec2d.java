package dev.aperture.core.geometry;

/**
 * 2D vector in logical millimeter space (profile / UV plane).
 */
public record Vec2d(double u, double v) {
	public static final Vec2d ZERO = new Vec2d(0, 0);

	public Vec2d add(Vec2d other) {
		return new Vec2d(u + other.u, v + other.v);
	}
}
