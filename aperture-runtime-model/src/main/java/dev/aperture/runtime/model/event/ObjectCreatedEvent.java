package dev.aperture.runtime.model.event;

import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;

import java.util.Objects;

public record ObjectCreatedEvent(ArchitecturalObjectInstance instance) implements ArchitecturalEvent {
	public ObjectCreatedEvent { Objects.requireNonNull(instance, "instance"); }
}
