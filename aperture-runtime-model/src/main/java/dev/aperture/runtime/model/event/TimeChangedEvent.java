package dev.aperture.runtime.model.event;

import java.time.Instant;
import java.util.Objects;

public record TimeChangedEvent(Instant previousTime, Instant currentTime) implements ArchitecturalEvent {
	public TimeChangedEvent {
		Objects.requireNonNull(previousTime, "previousTime");
		Objects.requireNonNull(currentTime, "currentTime");
	}
}
