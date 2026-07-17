package dev.aperture.runtime.model.event;

@FunctionalInterface
public interface EventHandler<T extends ArchitecturalEvent> {
	void handle(EventEnvelope<T> envelope);
}
