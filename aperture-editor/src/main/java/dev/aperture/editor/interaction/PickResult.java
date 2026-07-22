package dev.aperture.editor.interaction;

import dev.aperture.editor.model.selection.ComponentPath;
import dev.aperture.math.Vec3d;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.Optional;

public record PickResult(ArchitecturalObjectId objectId, Optional<ComponentPath> componentPath,
	Optional<String> manipulatorId, PickCandidate.HitKind hitKind, Vec3d worldPosition,
	Vec3d worldNormal, double distance, PickPriority priority, String sourceId) { }
