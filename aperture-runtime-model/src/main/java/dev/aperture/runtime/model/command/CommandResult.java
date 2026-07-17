package dev.aperture.runtime.model.command;

import dev.aperture.runtime.model.event.ArchitecturalEvent;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.world.WorldEffect;

import java.util.List;

/** Pure command evaluation result; orchestration applies these intents after acceptance. */
public record CommandResult(
	Status status,
	List<StatePatch> statePatches,
	List<ArchitecturalEvent> events,
	List<WorldEffect> worldEffects,
	List<CommandDiagnostic> diagnostics
) {
	public enum Status { ACCEPTED, REJECTED }

	public CommandResult {
		statePatches = List.copyOf(statePatches);
		events = List.copyOf(events);
		worldEffects = List.copyOf(worldEffects);
		diagnostics = List.copyOf(diagnostics);
		if (status == Status.REJECTED && diagnostics.stream().noneMatch(d -> d.severity() == CommandDiagnostic.Severity.ERROR)) {
			throw new IllegalArgumentException("Rejected command requires an error diagnostic");
		}
	}

	public static CommandResult accepted(List<StatePatch> patches) {
		return new CommandResult(Status.ACCEPTED, patches, List.of(), List.of(), List.of());
	}

	public static CommandResult rejected(String code, String message) {
		return new CommandResult(Status.REJECTED, List.of(), List.of(), List.of(),
			List.of(CommandDiagnostic.error(code, message)));
	}
}
