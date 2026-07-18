package dev.aperture.runtime.transaction;

import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.command.CommandResult;
import dev.aperture.runtime.model.event.ArchitecturalEvent;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.world.WorldEffect;

import java.util.List;

/** Complete immutable intent produced before an authoritative runtime commit. */
public record RuntimeMutation(
	List<StatePatch> statePatches,
	List<ArchitecturalEvent> events,
	List<WorldEffect> worldEffects,
	List<ArchitecturalCommand> scheduledCommands
) {
	public static final RuntimeMutation EMPTY = new RuntimeMutation(List.of(), List.of(), List.of(), List.of());

	public RuntimeMutation {
		statePatches = List.copyOf(statePatches);
		events = List.copyOf(events);
		worldEffects = List.copyOf(worldEffects);
		scheduledCommands = List.copyOf(scheduledCommands);
	}

	public static RuntimeMutation from(CommandResult result) {
		return new RuntimeMutation(result.statePatches(), result.events(), result.worldEffects(), List.of());
	}

	public boolean isEmpty() {
		return statePatches.isEmpty() && events.isEmpty() && worldEffects.isEmpty() && scheduledCommands.isEmpty();
	}
}
