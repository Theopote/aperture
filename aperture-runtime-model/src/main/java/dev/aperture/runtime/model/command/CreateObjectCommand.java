package dev.aperture.runtime.model.command;

import dev.aperture.runtime.model.event.ObjectRef;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import java.util.Objects;

public record CreateObjectCommand(ArchitecturalObjectInstance instance) implements ArchitecturalCommand {
	public CreateObjectCommand { Objects.requireNonNull(instance, "instance"); }
	@Override public String commandType() { return "aperture:create_object"; }
	@Override public ObjectRef target() { return new ObjectRef(instance.objectId()); }
}
