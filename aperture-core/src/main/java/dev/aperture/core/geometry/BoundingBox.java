package dev.aperture.core.geometry;

/**
 * Axis-aligned bounding box in logical millimeter space.
 */
public record BoundingBox(Vec3d min, Vec3d max) {
	public static BoundingBox fromSize(double width, double height, double depth) {
		return new BoundingBox(Vec3d.ZERO, new Vec3d(width, height, depth));
	}

	public double width() {
		return max.x() - min.x();
	}

	public double height() {
		return max.y() - min.y();
	}

	public double depth() {
		return max.z() - min.z();
	}
}
