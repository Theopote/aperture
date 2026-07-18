package dev.aperture.editor.model.selection;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.Objects;

public record ComponentSelection(ArchitecturalObjectId objectId, ComponentPath path) {
	public ComponentSelection { Objects.requireNonNull(objectId); Objects.requireNonNull(path); }
}
