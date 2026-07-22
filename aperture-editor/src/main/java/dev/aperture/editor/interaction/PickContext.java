package dev.aperture.editor.interaction;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.Optional;

public record PickContext(Optional<ArchitecturalObjectId> selectedObject) {
	public PickContext { selectedObject = selectedObject == null ? Optional.empty() : selectedObject; }
	public static PickContext empty() { return new PickContext(Optional.empty()); }
}
