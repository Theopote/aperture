package dev.aperture.runtime.kinematic;

import dev.aperture.geometry.kinematic.ComponentPath;
import dev.aperture.geometry.kinematic.KinematicEvaluator;
import dev.aperture.geometry.kinematic.KinematicPart;
import dev.aperture.math.Transform3d;
import dev.aperture.runtime.lifecycle.RuntimeObjectSession;
import dev.aperture.runtime.model.state.StateValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** One immutable evaluated pose shared by rendering, collision, and picking. */
public record KinematicPose(Map<ComponentPath, Transform3d> transforms) {
	public static final KinematicPose IDENTITY = new KinematicPose(Map.of());

	public KinematicPose {
		transforms = Map.copyOf(transforms);
	}

	public static KinematicPose evaluate(RuntimeObjectSession session) {
		Objects.requireNonNull(session, "session");
		Map<ComponentPath, Transform3d> transforms = new LinkedHashMap<>();
		for (KinematicPart part : session.kinematics().parts()) {
			StateValue value = session.state().value(part.motion().driver());
			if (!(value instanceof StateValue.NumberValue number)) {
				throw new IllegalArgumentException("Kinematic driver must be numeric: " + part.motion().driver());
			}
			transforms.put(part.path(), KinematicEvaluator.evaluate(part,
				Map.of(part.motion().driver(), number.value())));
		}
		return new KinematicPose(transforms);
	}

	/** Finds the nearest dynamic ancestor transform for a generated component path. */
	public Transform3d transformFor(ComponentPath component) {
		ComponentPath best = null;
		for (ComponentPath root : transforms.keySet()) {
			if (root.contains(component) && (best == null || root.value().length() > best.value().length())) best = root;
		}
		return best == null ? Transform3d.identity() : transforms.get(best);
	}
}
