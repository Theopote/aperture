package dev.aperture.runtime.model.command;

import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.event.WorldRef;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Authority, causality, and optimistic revision data surrounding a command. */
public record CommandEnvelope<T extends ArchitecturalCommand>(
	UUID commandId,
	T command,
	ActorRef actor,
	WorldRef world,
	long expectedObjectRevision,
	Instant timestamp,
	UUID correlationId,
	UUID causationId,
	Map<String, String> metadata
) {
	public CommandEnvelope {
		Objects.requireNonNull(commandId, "commandId");
		Objects.requireNonNull(command, "command");
		Objects.requireNonNull(actor, "actor");
		Objects.requireNonNull(world, "world");
		if (expectedObjectRevision < 0) throw new IllegalArgumentException("expectedObjectRevision must be non-negative");
		Objects.requireNonNull(timestamp, "timestamp");
		Objects.requireNonNull(correlationId, "correlationId");
		metadata = Map.copyOf(metadata);
	}

	public static <T extends ArchitecturalCommand> CommandEnvelope<T> create(
		T command, ActorRef actor, WorldRef world, long expectedRevision, Instant timestamp
	) {
		return new CommandEnvelope<>(UUID.randomUUID(), command, actor, world, expectedRevision,
			timestamp, UUID.randomUUID(), null, Map.of());
	}
}
