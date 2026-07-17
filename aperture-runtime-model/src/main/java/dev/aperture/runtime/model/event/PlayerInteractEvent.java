package dev.aperture.runtime.model.event;

import java.util.Objects;

public record PlayerInteractEvent(ObjectRef target, String interactionId, SpatialRef hit) implements ArchitecturalEvent {
	public PlayerInteractEvent {
		Objects.requireNonNull(target, "target");
		Objects.requireNonNull(interactionId, "interactionId");
		Objects.requireNonNull(hit, "hit");
		if (interactionId.isBlank()) throw new IllegalArgumentException("interactionId must not be blank");
	}
}
