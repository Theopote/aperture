package dev.aperture.runtime.model.event;

public interface EventDispatcher {
	<T extends ArchitecturalEvent> EventSubscription subscribe(EventType<T> type, EventHandler<T> handler);
	<T extends ArchitecturalEvent> void dispatch(EventEnvelope<T> envelope);
}
