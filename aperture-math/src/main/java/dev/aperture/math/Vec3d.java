package dev.aperture.math;

/**
 * 3D vector in logical millimeter space.
 */
public record Vec3d(double x, double y, double z) {
	public static final Vec3d ZERO = new Vec3d(0, 0, 0);

	public Vec3d add(Vec3d other) {
		return new Vec3d(x + other.x, y + other.y, z + other.z);
	}

	public Vec3d subtract(Vec3d other) {
		return new Vec3d(x - other.x, y - other.y, z - other.z);
	}

	public Vec3d scale(double factor) {
		return new Vec3d(x * factor, y * factor, z * factor);
	}

	public double dot(Vec3d other) {
		return x * other.x + y * other.y + z * other.z;
	}

	public Vec3d cross(Vec3d other) {
		return new Vec3d(
			y * other.z - z * other.y,
			z * other.x - x * other.z,
			x * other.y - y * other.x
		);
	}

	public double lengthSquared() {
		return dot(this);
	}

	public Vec3d normalize() {
		double length = Math.sqrt(lengthSquared());
		if (length < 1.0e-9) {
			return ZERO;
		}
		return scale(1.0 / length);
	}
}
