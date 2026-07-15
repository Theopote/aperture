package dev.aperture.opening.geometry.kinematic;

import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.geometry.Vec3d;

/**
 * Solves operable panel transforms from hinge side and open angle.
 */
public final class PanelKinematics {
	private static final Vec3d AXIS_X = new Vec3d(1, 0, 0);
	private static final Vec3d AXIS_Y = new Vec3d(0, 1, 0);

	private PanelKinematics() {
	}

	public static Transform3d solve(
		String hinge,
		double frameFace,
		double width,
		double height,
		double openAngleDegrees
	) {
		Vec3d axisOrigin = switch (hinge.toLowerCase()) {
			case "left" -> new Vec3d(frameFace, 0, 0);
			case "right" -> new Vec3d(width - frameFace, 0, 0);
			case "top" -> new Vec3d(0, height - frameFace, 0);
			case "bottom" -> new Vec3d(0, frameFace, 0);
			default -> throw new IllegalArgumentException("Unknown panel hinge: " + hinge);
		};
		return solveAtHinge(hinge, axisOrigin, openAngleDegrees);
	}

	public static Transform3d solveAtHinge(String hinge, Vec3d axisOrigin, double openAngleDegrees) {
		double radians = Math.toRadians(openAngleDegrees);
		if (Math.abs(radians) < 1.0e-9) {
			return Transform3d.identity();
		}

		return switch (hinge.toLowerCase()) {
			case "left" -> Transform3d.rotateAboutAxis(axisOrigin, AXIS_Y, -radians);
			case "right" -> Transform3d.rotateAboutAxis(axisOrigin, AXIS_Y, radians);
			case "top" -> Transform3d.rotateAboutAxis(axisOrigin, AXIS_X, radians);
			case "bottom" -> Transform3d.rotateAboutAxis(axisOrigin, AXIS_X, -radians);
			default -> throw new IllegalArgumentException("Unknown panel hinge: " + hinge);
		};
	}
}
