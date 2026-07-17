package dev.aperture.runtime.model.event;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.Objects;

public record ObjectRef(ArchitecturalObjectId objectId) {
	public ObjectRef { Objects.requireNonNull(objectId, "objectId"); }
}
