package dev.aperture.runtime.model.event;

import java.util.Objects;

public record RedstoneSignalChangedEvent(ObjectRef target, int previousSignal, int currentSignal) implements ArchitecturalEvent {
	public RedstoneSignalChangedEvent {
		Objects.requireNonNull(target, "target");
		if (previousSignal < 0 || previousSignal > 15 || currentSignal < 0 || currentSignal > 15) {
			throw new IllegalArgumentException("Signal must be between 0 and 15");
		}
	}
}
