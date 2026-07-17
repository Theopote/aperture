package dev.aperture.runtime.model.command;

import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.object.HostBinding;
import java.util.Objects;

public record AttachToHostCommand(ObjectRef target, HostBinding binding) implements ArchitecturalCommand {
	public AttachToHostCommand { Objects.requireNonNull(target, "target"); Objects.requireNonNull(binding, "binding"); }
	@Override public String commandType() { return "aperture:attach_to_host"; }
}
