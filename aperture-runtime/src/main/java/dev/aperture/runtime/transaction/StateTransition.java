package dev.aperture.runtime.transaction;

import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.state.StatePatch;

import java.util.Objects;
import java.util.Optional;

/** Validated candidate state transition built before repository replacement. */
public record StateTransition(RuntimeState previous, RuntimeState current, Optional<StatePatch> appliedPatch) {
	public StateTransition {
		Objects.requireNonNull(previous, "previous");
		Objects.requireNonNull(current, "current");
		Objects.requireNonNull(appliedPatch, "appliedPatch");
	}

	public boolean changed() { return !previous.equals(current); }
}
