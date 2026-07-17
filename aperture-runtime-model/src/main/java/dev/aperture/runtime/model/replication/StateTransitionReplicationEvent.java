package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.state.StateRevision;

import java.util.Objects;
import java.util.Set;

/** Public notification accompanying a state delta, without internal state values. */
public record StateTransitionReplicationEvent(
	StateRevision previousRevision, StateRevision currentRevision, Set<String> changedProperties
) implements ReplicatedEvent {
	public StateTransitionReplicationEvent {
		Objects.requireNonNull(previousRevision, "previousRevision");
		Objects.requireNonNull(currentRevision, "currentRevision");
		if (currentRevision.value() != previousRevision.value() + 1) {
			throw new IllegalArgumentException("State transition must be contiguous");
		}
		changedProperties = Set.copyOf(changedProperties);
	}

	@Override
	public String eventType() { return "state_transition"; }
}
