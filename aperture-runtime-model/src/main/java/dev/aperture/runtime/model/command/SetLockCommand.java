package dev.aperture.runtime.model.command;

import dev.aperture.runtime.model.event.ObjectRef;
import java.util.Objects;

public record SetLockCommand(ObjectRef target, boolean locked) implements ArchitecturalCommand {
	public SetLockCommand { Objects.requireNonNull(target, "target"); }
	@Override public String commandType() { return "aperture:set_lock"; }
}
