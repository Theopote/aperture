package dev.aperture.runtime.model.state;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Atomic validated state changes based on one expected revision. */
public record StatePatch(StateRevision expectedRevision, Map<String, StateValue> updates, Instant timestamp) {
	public StatePatch {
		Objects.requireNonNull(expectedRevision, "expectedRevision");
		updates = Map.copyOf(updates);
		if (updates.isEmpty()) throw new IllegalArgumentException("State patch must contain at least one update");
		Objects.requireNonNull(timestamp, "timestamp");
	}
}
