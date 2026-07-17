package dev.aperture.runtime.model.event;

import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.state.StateRevision;

import java.util.Objects;

public record StateChangedEvent(
	ObjectRef target, StateRevision previousRevision, StateRevision currentRevision, StatePatch appliedPatch
) implements ArchitecturalEvent {
	public StateChangedEvent {
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(previousRevision, "previousRevision");
		Objects.requireNonNull(currentRevision, "currentRevision");
		Objects.requireNonNull(appliedPatch, "appliedPatch");
		if (currentRevision.value() != previousRevision.value() + 1) {
			throw new IllegalArgumentException("StateChangedEvent must describe one revision transition");
		}
	}
}
