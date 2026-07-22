package dev.aperture.editor.interaction;

import dev.aperture.math.Vec3d;

import java.util.Optional;

/** Pure perspective projection used by host adapters and projection tests. */
public final class PerspectiveProjector {
	private static final double NEAR_EPSILON = 1.0e-6;

	public Optional<ScreenPoint> project(Vec3d point, View view) {
		Vec3d forward = view.forward().normalize();
		Vec3d right = forward.cross(view.nominalUp()).normalize();
		if (right.lengthSquared() < NEAR_EPSILON) return Optional.empty();
		Vec3d up = right.cross(forward).normalize();
		Vec3d relative = point.subtract(view.origin());
		double depth = relative.dot(forward);
		if (depth <= NEAR_EPSILON) return Optional.empty();
		double focalY = view.heightPixels() / (2.0 * Math.tan(Math.toRadians(view.verticalFovDegrees()) / 2.0));
		double x = view.widthPixels() / 2.0 + relative.dot(right) * focalY / depth;
		double y = view.heightPixels() / 2.0 - relative.dot(up) * focalY / depth;
		return Double.isFinite(x) && Double.isFinite(y) ? Optional.of(new ScreenPoint(x, y)) : Optional.empty();
	}

	public record View(Vec3d origin, Vec3d forward, Vec3d nominalUp,
		double verticalFovDegrees, double widthPixels, double heightPixels) {
		public View {
			if (verticalFovDegrees <= 0 || verticalFovDegrees >= 180) throw new IllegalArgumentException("invalid FOV");
			if (widthPixels <= 0 || heightPixels <= 0) throw new IllegalArgumentException("invalid viewport");
		}
	}
}
