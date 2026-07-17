package dev.aperture.math;

import java.util.Objects;

/** Right-handed orthonormal frame used for host-local insertion coordinates. */
public record LocalFrame(Vec3d origin, Vec3d xAxis, Vec3d yAxis, Vec3d zAxis) {
	private static final double EPSILON = 1.0e-6;

	public LocalFrame {
		Objects.requireNonNull(origin, "origin");
		xAxis = normalized(xAxis, "xAxis");
		yAxis = normalized(yAxis, "yAxis");
		zAxis = normalized(zAxis, "zAxis");
		if (Math.abs(xAxis.dot(yAxis)) > EPSILON || Math.abs(xAxis.dot(zAxis)) > EPSILON
			|| Math.abs(yAxis.dot(zAxis)) > EPSILON || xAxis.cross(yAxis).dot(zAxis) < 1.0 - EPSILON) {
			throw new IllegalArgumentException("Local frame axes must form a right-handed orthonormal basis");
		}
	}

	public static LocalFrame identity() {
		return new LocalFrame(Vec3d.ZERO, new Vec3d(1, 0, 0), new Vec3d(0, 1, 0), new Vec3d(0, 0, 1));
	}

	public static LocalFrame fromSurfaceNormal(Vec3d origin, Vec3d surfaceNormal) {
		Vec3d z = normalized(surfaceNormal, "surfaceNormal");
		Vec3d reference = Math.abs(z.y()) < 0.9 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
		Vec3d x = reference.cross(z).normalize();
		return new LocalFrame(origin, x, z.cross(x).normalize(), z);
	}

	public Vec3d surfaceNormal() { return zAxis; }
	public Vec3d exteriorDirection() { return zAxis; }
	public Vec3d interiorDirection() { return zAxis.scale(-1); }

	public Vec3d toWorld(Vec3d localCoordinates) {
		return origin.add(xAxis.scale(localCoordinates.x())).add(yAxis.scale(localCoordinates.y())).add(zAxis.scale(localCoordinates.z()));
	}

	private static Vec3d normalized(Vec3d vector, String name) {
		Objects.requireNonNull(vector, name);
		if (!Double.isFinite(vector.x()) || !Double.isFinite(vector.y()) || !Double.isFinite(vector.z())) {
			throw new IllegalArgumentException(name + " must be finite");
		}
		Vec3d normalized = vector.normalize();
		if (normalized.equals(Vec3d.ZERO)) throw new IllegalArgumentException(name + " must not be zero");
		return normalized;
	}
}
