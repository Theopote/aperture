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

	public static BoundingBox ofPoints(Vec3d... points) {
		if (points.length == 0) {
			throw new IllegalArgumentException("points must not be empty");
		}
		double minX = points[0].x();
		double minY = points[0].y();
		double minZ = points[0].z();
		double maxX = minX;
		double maxY = minY;
		double maxZ = minZ;
		for (int i = 1; i < points.length; i++) {
			Vec3d point = points[i];
			minX = Math.min(minX, point.x());
			minY = Math.min(minY, point.y());
			minZ = Math.min(minZ, point.z());
			maxX = Math.max(maxX, point.x());
			maxY = Math.max(maxY, point.y());
			maxZ = Math.max(maxZ, point.z());
		}
		return new BoundingBox(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ));
	}

	public BoundingBox union(BoundingBox other) {
		return new BoundingBox(
			new Vec3d(
				Math.min(min.x(), other.min.x()),
				Math.min(min.y(), other.min.y()),
				Math.min(min.z(), other.min.z())
			),
			new Vec3d(
				Math.max(max.x(), other.max.x()),
				Math.max(max.y(), other.max.y()),
				Math.max(max.z(), other.max.z())
			)
		);
	}
}
