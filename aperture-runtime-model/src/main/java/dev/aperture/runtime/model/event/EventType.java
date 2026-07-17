package dev.aperture.runtime.model.event;

import java.util.Objects;
import java.util.regex.Pattern;

/** Stable namespaced identity paired with the event payload type. */
public record EventType<T extends ArchitecturalEvent>(String id, Class<T> payloadType) {
	private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

	public EventType {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(payloadType, "payloadType");
		if (!ID.matcher(id).matches()) throw new IllegalArgumentException("Event ID must be namespace:path: " + id);
	}

	public static <T extends ArchitecturalEvent> EventType<T> of(String id, Class<T> payloadType) {
		return new EventType<>(id, payloadType);
	}

	public T cast(ArchitecturalEvent event) { return payloadType.cast(event); }
}
