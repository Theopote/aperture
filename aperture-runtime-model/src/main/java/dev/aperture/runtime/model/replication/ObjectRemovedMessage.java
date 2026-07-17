package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.time.Instant;
import java.util.Objects;

/** Authoritative tombstone for a removed object. */
public record ObjectRemovedMessage(
	int protocolVersion, ArchitecturalObjectId objectId, long finalObjectRevision, Instant timestamp
) implements ReplicationMessage {
	public ObjectRemovedMessage {
		if (protocolVersion < 1) throw new IllegalArgumentException("protocolVersion must be >= 1");
		Objects.requireNonNull(objectId, "objectId");
		if (finalObjectRevision < 0) throw new IllegalArgumentException("finalObjectRevision must be non-negative");
		Objects.requireNonNull(timestamp, "timestamp");
	}
}
