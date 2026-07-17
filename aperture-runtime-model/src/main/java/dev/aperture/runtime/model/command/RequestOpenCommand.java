package dev.aperture.runtime.model.command;

import dev.aperture.runtime.model.event.ObjectRef;
import java.util.Objects;

public record RequestOpenCommand(ObjectRef target) implements ArchitecturalCommand {
	public RequestOpenCommand { Objects.requireNonNull(target, "target"); }
	@Override public String commandType() { return "aperture:request_open"; }
}
