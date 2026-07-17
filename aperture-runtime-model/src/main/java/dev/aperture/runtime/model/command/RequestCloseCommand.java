package dev.aperture.runtime.model.command;

import dev.aperture.runtime.model.event.ObjectRef;
import java.util.Objects;

public record RequestCloseCommand(ObjectRef target) implements ArchitecturalCommand {
	public RequestCloseCommand { Objects.requireNonNull(target, "target"); }
	@Override public String commandType() { return "aperture:request_close"; }
}
