package dev.aperture.runtime.model.command;

import dev.aperture.math.Transform3d;
import dev.aperture.runtime.model.event.ObjectRef;
import java.util.Objects;

public record TransformObjectCommand(ObjectRef target, Transform3d transform) implements ArchitecturalCommand {
	public TransformObjectCommand { Objects.requireNonNull(target, "target"); Objects.requireNonNull(transform, "transform"); }
	@Override public String commandType() { return "aperture:transform_object"; }
}
