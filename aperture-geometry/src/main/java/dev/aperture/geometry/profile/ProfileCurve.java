package dev.aperture.geometry.profile;

import dev.aperture.core.geometry.Vec2d;

import java.util.Arrays;
import java.util.List;

/**
 * Closed 2D polyline profile in millimeter space.
 */
public record ProfileCurve(List<Vec2d> points) {
	public ProfileCurve {
		if (points.size() < 3) {
			throw new IllegalArgumentException("profile must have at least 3 points");
		}
		points = List.copyOf(points);
	}

	public static ProfileCurve rectangle(double minU, double minV, double maxU, double maxV) {
		return new ProfileCurve(List.of(
			new Vec2d(minU, minV),
			new Vec2d(maxU, minV),
			new Vec2d(maxU, maxV),
			new Vec2d(minU, maxV)
		));
	}

	public static ProfileCurve fromPoints(List<Vec2d> points) {
		return new ProfileCurve(points);
	}

	public ProfileBounds bounds() {
		double minU = Double.POSITIVE_INFINITY;
		double minV = Double.POSITIVE_INFINITY;
		double maxU = Double.NEGATIVE_INFINITY;
		double maxV = Double.NEGATIVE_INFINITY;
		for (Vec2d point : points) {
			minU = Math.min(minU, point.u());
			minV = Math.min(minV, point.v());
			maxU = Math.max(maxU, point.u());
			maxV = Math.max(maxV, point.v());
		}
		return new ProfileBounds(minU, minV, maxU, maxV);
	}

	public int segmentCount() {
		return points.size();
	}

	public Vec2d point(int index) {
		return points.get(Math.floorMod(index, points.size()));
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ProfileCurve other && points.equals(other.points);
	}

	@Override
	public int hashCode() {
		return points.hashCode();
	}

	@Override
	public String toString() {
		return "ProfileCurve" + Arrays.toString(points.toArray());
	}
}
