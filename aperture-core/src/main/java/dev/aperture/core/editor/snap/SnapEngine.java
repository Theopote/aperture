package dev.aperture.core.editor.snap;

import dev.aperture.core.geometry.Vec3d;

import java.util.List;

/**
 * Snaps drag positions to nearby {@link SnapPoint}s and grid increments.
 */
public final class SnapEngine {
	public Vec3d snap(Vec3d candidate, List<SnapPoint> targets, SnapPolicy policy) {
		if (!policy.enabled()) {
			return candidate;
		}

		Vec3d snapped = snapToGrid(candidate, policy.gridStepMm());
		Vec3d best = snapped;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (SnapPoint target : targets) {
			double distance = distance(candidate, target.position());
			if (distance <= policy.toleranceMm() && distance < bestDistance) {
				bestDistance = distance;
				best = target.position();
			}
		}
		return best;
	}

	private static Vec3d snapToGrid(Vec3d point, double step) {
		if (step <= 0) {
			return point;
		}
		return new Vec3d(
			snapAxis(point.x(), step),
			snapAxis(point.y(), step),
			snapAxis(point.z(), step)
		);
	}

	private static double snapAxis(double value, double step) {
		return Math.round(value / step) * step;
	}

	private static double distance(Vec3d a, Vec3d b) {
		return a.subtract(b).length();
	}
}
