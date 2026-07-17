package dev.aperture.runtime.model.behavior;

import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.capability.CapabilityProvider;
import dev.aperture.runtime.model.event.ArchitecturalEvent;
import dev.aperture.runtime.model.event.EventEnvelope;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.world.WorldQuery;

import java.util.Objects;

/** Immutable inputs available during one behavior evaluation. */
public record BehaviorContext(
	ArchitecturalObjectInstance instance,
	RuntimeState state,
	ParameterSet parameters,
	CapabilityProvider capabilities,
	EventEnvelope<? extends ArchitecturalEvent> event,
	WorldQuery worldQuery
) {
	public BehaviorContext {
		Objects.requireNonNull(instance, "instance");
		Objects.requireNonNull(state, "state");
		Objects.requireNonNull(parameters, "parameters");
		Objects.requireNonNull(capabilities, "capabilities");
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(worldQuery, "worldQuery");
	}
}
