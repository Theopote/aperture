package dev.aperture.runtime.model.command;

import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.state.StatePatch;
import java.util.Objects;

public record SetStateCommand(ObjectRef target, StatePatch patch) implements ArchitecturalCommand {
	public SetStateCommand { Objects.requireNonNull(target, "target"); Objects.requireNonNull(patch, "patch"); }
	@Override public String commandType() { return "aperture:set_state"; }
}
