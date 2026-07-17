package dev.aperture.runtime.model.event;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic in-process dispatcher intended for runtime orchestration and tests. */
public final class SynchronousEventDispatcher implements EventDispatcher {
	private final Map<EventType<?>, List<Registration<?>>> handlers = new LinkedHashMap<>();

	@Override
	public synchronized <T extends ArchitecturalEvent> EventSubscription subscribe(
		EventType<T> type, EventHandler<T> handler
	) {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(handler, "handler");
		Registration<T> registration = new Registration<>(type, handler);
		handlers.computeIfAbsent(type, ignored -> new ArrayList<>()).add(registration);
		return registration;
	}

	@Override
	public <T extends ArchitecturalEvent> void dispatch(EventEnvelope<T> envelope) {
		List<Registration<?>> snapshot;
		synchronized (this) {
			snapshot = List.copyOf(handlers.getOrDefault(envelope.type(), List.of()));
		}
		for (Registration<?> registration : snapshot) registration.dispatch(envelope);
	}

	private final class Registration<T extends ArchitecturalEvent> implements EventSubscription {
		private final EventType<T> type;
		private final EventHandler<T> handler;
		private boolean active = true;

		private Registration(EventType<T> type, EventHandler<T> handler) {
			this.type = type;
			this.handler = handler;
		}

		private void dispatch(EventEnvelope<?> envelope) {
			if (!active) return;
			T payload = type.cast(envelope.event());
			@SuppressWarnings("unchecked") EventEnvelope<T> typed = (EventEnvelope<T>) envelope;
			if (typed.event() != payload) throw new IllegalStateException("Event payload identity changed");
			handler.handle(typed);
		}

		@Override public synchronized boolean active() { return active; }

		@Override
		public void close() {
			synchronized (SynchronousEventDispatcher.this) {
				if (!active) return;
				active = false;
				handlers.getOrDefault(type, List.of()).remove(this);
			}
		}
	}
}
