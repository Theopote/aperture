package dev.aperture.runtime.model.event;

import java.util.Objects;

public record NeighborChangedEvent(ObjectRef target, SpatialRef neighbor) implements ArchitecturalEvent {
	public NeighborChangedEvent {
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(neighbor, "neighbor");
	}
}
