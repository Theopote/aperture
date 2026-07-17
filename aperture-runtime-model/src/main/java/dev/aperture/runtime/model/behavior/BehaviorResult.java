package dev.aperture.runtime.model.behavior;

import dev.aperture.runtime.model.command.ArchitecturalCommand;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.world.WorldEffect;

import java.util.ArrayList;
import java.util.List;

/** Declarative behavior output; it never mutates runtime or world state directly. */
public record BehaviorResult(
	List<ArchitecturalCommand> commands,
	List<StatePatch> statePatches,
	List<WorldEffect> worldEffects,
	List<BehaviorDiagnostic> diagnostics
) {
	private static final BehaviorResult EMPTY = new BehaviorResult(List.of(), List.of(), List.of(), List.of());

	public BehaviorResult {
		commands = List.copyOf(commands);
		statePatches = List.copyOf(statePatches);
		worldEffects = List.copyOf(worldEffects);
		diagnostics = List.copyOf(diagnostics);
	}

	public static BehaviorResult empty() { return EMPTY; }

	public BehaviorResult combine(BehaviorResult other) {
		return new BehaviorResult(
			concat(commands, other.commands),
			concat(statePatches, other.statePatches),
			concat(worldEffects, other.worldEffects),
			concat(diagnostics, other.diagnostics)
		);
	}

	private static <T> List<T> concat(List<T> first, List<T> second) {
		List<T> combined = new ArrayList<>(first.size() + second.size());
		combined.addAll(first);
		combined.addAll(second);
		return List.copyOf(combined);
	}
}
