package dev.aperture.runtime.model.event;

import java.util.Objects;
import java.util.UUID;

public record CommandAppliedEvent(UUID commandId, ObjectRef target, long resultingRevision) implements ArchitecturalEvent {
	public CommandAppliedEvent {
		Objects.requireNonNull(commandId, "commandId");
		Objects.requireNonNull(target, "target");
		if (resultingRevision < 0) throw new IllegalArgumentException("resultingRevision must be non-negative");
	}
}
