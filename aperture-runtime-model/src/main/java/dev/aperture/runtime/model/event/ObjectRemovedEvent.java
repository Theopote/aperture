package dev.aperture.runtime.model.event;

import java.util.Objects;

public record ObjectRemovedEvent(ObjectRef target, long finalRevision) implements ArchitecturalEvent {
	public ObjectRemovedEvent {
		Objects.requireNonNull(target, "target");
		if (finalRevision < 0) throw new IllegalArgumentException("finalRevision must be non-negative");
	}
}
