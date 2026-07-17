package dev.aperture.runtime.model.command;

import dev.aperture.runtime.model.capability.CapabilityProvider;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.world.WorldQuery;

import java.util.Objects;

/** Immutable authoritative snapshot used to validate and evaluate a command. */
public record CommandContext(
	ArchitecturalObjectInstance instance,
	RuntimeState state,
	CapabilityProvider capabilities,
	WorldQuery worldQuery
) {
	public CommandContext {
		Objects.requireNonNull(instance, "instance");
		Objects.requireNonNull(state, "state");
		Objects.requireNonNull(capabilities, "capabilities");
		Objects.requireNonNull(worldQuery, "worldQuery");
	}
}
