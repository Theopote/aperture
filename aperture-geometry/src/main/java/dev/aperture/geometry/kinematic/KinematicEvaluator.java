package dev.aperture.geometry.kinematic;

import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;

import java.util.Map;

/** Evaluates dynamic transforms from state without recompiling geometry or mesh. */
public final class KinematicEvaluator {
	private KinematicEvaluator() { }

	public static Transform3d evaluate(KinematicPart part, Map<String, Double> stateDrivers) {
		Double ratio = stateDrivers.get(part.motion().driver());
		if (ratio == null) throw new IllegalArgumentException("Missing kinematic state driver: " + part.motion().driver());
		double value = part.motion().valueAt(ratio);
		Transform3d base = part.localTransform();
		if (part.motion().movementType() == MovementType.ROTATE) {
			return new Transform3d(base.origin(), base.facing(), part.pivot().position(), part.motion().axis(), value);
		}
		Vec3d translated = base.origin().add(part.motion().axis().scale(value));
		return new Transform3d(translated, base.facing(), base.rotationAxisOrigin(),
			base.rotationAxisDirection(), base.rotationRadians());
	}
}
