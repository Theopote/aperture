package dev.aperture.runtime.model.command;

import dev.aperture.runtime.model.event.ObjectRef;
import java.util.Objects;

public record DetachFromHostCommand(ObjectRef target, String featureId) implements ArchitecturalCommand {
	public DetachFromHostCommand { Objects.requireNonNull(target, "target"); Objects.requireNonNull(featureId, "featureId"); }
	@Override public String commandType() { return "aperture:detach_from_host"; }
}
