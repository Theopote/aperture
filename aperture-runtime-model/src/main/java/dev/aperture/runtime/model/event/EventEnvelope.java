package dev.aperture.runtime.model.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Ordered transport unit for an immutable architectural event. */
public record EventEnvelope<T extends ArchitecturalEvent>(
	UUID eventId,
	EventType<T> type,
	T event,
	EventContext context,
	Instant timestamp,
	long sequence
) {
	public EventEnvelope {
		Objects.requireNonNull(eventId, "eventId");
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(event, "event");
		Objects.requireNonNull(context, "context");
		Objects.requireNonNull(timestamp, "timestamp");
		if (!type.payloadType().isInstance(event)) throw new IllegalArgumentException("Event payload does not match " + type.id());
		if (sequence < 0) throw new IllegalArgumentException("Event sequence must be non-negative");
	}

	public static <T extends ArchitecturalEvent> EventEnvelope<T> create(
		EventType<T> type, T event, EventContext context, Instant timestamp, long sequence
	) {
		return new EventEnvelope<>(UUID.randomUUID(), type, event, context, timestamp, sequence);
	}
}
