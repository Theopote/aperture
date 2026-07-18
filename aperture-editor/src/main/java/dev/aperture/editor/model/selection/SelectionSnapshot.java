package dev.aperture.editor.model.selection;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.Optional;
import java.util.Set;

public record SelectionSnapshot(Set<ArchitecturalObjectId> objectIds, ArchitecturalObjectId primaryObject,
	Optional<ComponentSelection> primaryComponent, long revision) {
	public SelectionSnapshot { objectIds = Set.copyOf(objectIds); primaryComponent = primaryComponent == null ? Optional.empty() : primaryComponent; }
}
