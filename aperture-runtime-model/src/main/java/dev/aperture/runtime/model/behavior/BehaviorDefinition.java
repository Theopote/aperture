package dev.aperture.runtime.model.behavior;

import dev.aperture.runtime.model.event.EventType;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable description of one behavior and the events it accepts. */
public record BehaviorDefinition(
	BehaviorId id,
	int version,
	Set<EventType<?>> acceptedEvents,
	Map<String, Object> configuration
) {
	public BehaviorDefinition {
		Objects.requireNonNull(id, "id");
		if (version < 1) throw new IllegalArgumentException("Behavior version must be >= 1");
		acceptedEvents = Set.copyOf(acceptedEvents);
		configuration = Map.copyOf(configuration);
	}

	public boolean accepts(EventType<?> eventType) { return acceptedEvents.contains(eventType); }
}
