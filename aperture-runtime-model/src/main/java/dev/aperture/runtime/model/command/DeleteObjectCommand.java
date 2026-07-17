package dev.aperture.runtime.model.command;

import dev.aperture.runtime.model.event.ObjectRef;
import java.util.Objects;

public record DeleteObjectCommand(ObjectRef target) implements ArchitecturalCommand {
	public DeleteObjectCommand { Objects.requireNonNull(target, "target"); }
	@Override public String commandType() { return "aperture:delete_object"; }
}
