package dev.aperture.geometry.primitives;

/**
 * 3D vector in logical millimeter space.
 */
public record Vec3d(double x, double y, double z) {
	public static final Vec3d ZERO = new Vec3d(0, 0, 0);

	public Vec3d add(Vec3d other) {
		return new Vec3d(x + other.x, y + other.y, z + other.z);
	}
}
