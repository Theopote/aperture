package dev.aperture.runtime.transaction;

import dev.aperture.runtime.lifecycle.RuntimeObjectSession;
import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.event.ArchitecturalEvent;
import dev.aperture.runtime.model.world.WorldEffect;

import java.util.List;
import java.util.Objects;

/** Authoritative fact emitted only after the repository atomically accepts a candidate session. */
public record CommittedRuntimeChange(
	RuntimeObjectSession previous,
	RuntimeObjectSession current,
	StateTransition stateTransition,
	List<ArchitecturalEvent> events,
	List<WorldEffect> worldEffects,
	List<ArchitecturalCommand> scheduledCommands
) {
	public CommittedRuntimeChange {
		Objects.requireNonNull(previous, "previous");
		Objects.requireNonNull(current, "current");
		Objects.requireNonNull(stateTransition, "stateTransition");
		events = List.copyOf(events);
		worldEffects = List.copyOf(worldEffects);
		scheduledCommands = List.copyOf(scheduledCommands);
	}
}
