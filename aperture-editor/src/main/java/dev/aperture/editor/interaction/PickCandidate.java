package dev.aperture.editor.interaction;

import dev.aperture.editor.model.selection.ComponentPath;
import dev.aperture.math.Vec3d;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.Objects;
import java.util.Optional;

public record PickCandidate(
	ArchitecturalObjectId objectId,
	Optional<ComponentPath> componentPath,
	Optional<String> manipulatorId,
	HitKind hitKind,
	Vec3d worldPosition,
	Vec3d worldNormal,
	double distance,
	PickPriority priority
) {
	public enum HitKind { OBJECT, COMPONENT, MANIPULATOR, PREVIEW }

	public PickCandidate {
		Objects.requireNonNull(objectId, "objectId");
		componentPath = componentPath == null ? Optional.empty() : componentPath;
		manipulatorId = manipulatorId == null ? Optional.empty() : manipulatorId;
		Objects.requireNonNull(hitKind, "hitKind");
		Objects.requireNonNull(worldPosition, "worldPosition");
		Objects.requireNonNull(worldNormal, "worldNormal");
		Objects.requireNonNull(priority, "priority");
		if (!Double.isFinite(distance) || distance < 0.0) throw new IllegalArgumentException("invalid distance");
	}
}
