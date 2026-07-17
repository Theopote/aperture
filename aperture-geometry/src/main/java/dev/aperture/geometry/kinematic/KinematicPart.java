package dev.aperture.geometry.kinematic;

import dev.aperture.math.Transform3d;
import java.util.Objects;
import java.util.Optional;

/** Dynamic component root; descendants inherit its evaluated transform. */
public record KinematicPart(
	ComponentPath path,
	Transform3d localTransform,
	ComponentPath parentComponent,
	Pivot pivot,
	MotionDefinition motion
) {
	public KinematicPart {
		Objects.requireNonNull(path, "path");
		Objects.requireNonNull(localTransform, "localTransform");
		Objects.requireNonNull(pivot, "pivot");
		Objects.requireNonNull(motion, "motion");
		if (parentComponent != null && path.equals(parentComponent)) {
			throw new IllegalArgumentException("Kinematic part cannot parent itself");
		}
	}

	public Optional<ComponentPath> parent() { return Optional.ofNullable(parentComponent); }
}
