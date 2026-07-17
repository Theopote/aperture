package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Ordered batch of explicitly public events for one object. */
public record EventDeltaMessage(
	int protocolVersion,
	ArchitecturalObjectId objectId,
	long baseSequence,
	long resultingSequence,
	List<ReplicatedEvent> events,
	Instant timestamp
) implements ReplicationMessage {
	public EventDeltaMessage {
		if (protocolVersion < 1) throw new IllegalArgumentException("protocolVersion must be >= 1");
		Objects.requireNonNull(objectId, "objectId");
		if (baseSequence < 0 || resultingSequence < baseSequence) {
			throw new IllegalArgumentException("Invalid event sequence range");
		}
		events = List.copyOf(events);
		if (resultingSequence - baseSequence != events.size()) {
			throw new IllegalArgumentException("Sequence range must match event count");
		}
		Objects.requireNonNull(timestamp, "timestamp");
	}
}
