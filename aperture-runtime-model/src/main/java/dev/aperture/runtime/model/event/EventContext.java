package dev.aperture.runtime.model.event;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Causality, authority, and routing data surrounding an event payload. */
public record EventContext(
	WorldRef world,
	ActorRef actor,
	ObjectRef source,
	UUID correlationId,
	UUID causationId,
	Map<String, String> metadata
) {
	public EventContext {
		Objects.requireNonNull(world, "world");
		Objects.requireNonNull(correlationId, "correlationId");
		metadata = Map.copyOf(metadata);
	}

	public Optional<ActorRef> actorOptional() { return Optional.ofNullable(actor); }
	public Optional<ObjectRef> sourceOptional() { return Optional.ofNullable(source); }
	public Optional<UUID> causationOptional() { return Optional.ofNullable(causationId); }

	public static EventContext system(WorldRef world) {
		return new EventContext(world, ActorRef.SYSTEM, null, UUID.randomUUID(), null, Map.of());
	}
}
