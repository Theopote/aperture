package dev.aperture.geometry.kinematic;

import dev.aperture.math.Vec3d;
import java.util.Objects;

/** Maps one numeric runtime-state driver to a rotation or translation range. */
public record MotionDefinition(
	MovementType movementType,
	Vec3d axis,
	double minimum,
	double maximum,
	String driver
) {
	public MotionDefinition {
		Objects.requireNonNull(movementType, "movementType");
		Objects.requireNonNull(axis, "axis");
		Objects.requireNonNull(driver, "driver");
		if (axis.lengthSquared() == 0) throw new IllegalArgumentException("Motion axis must not be zero");
		if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum > maximum) {
			throw new IllegalArgumentException("Motion range must be finite and ordered");
		}
		if (driver.isBlank()) throw new IllegalArgumentException("Motion driver must not be blank");
		axis = axis.normalize();
	}

	public double valueAt(double ratio) {
		double clamped = Math.max(0, Math.min(1, ratio));
		return minimum + (maximum - minimum) * clamped;
	}
}
