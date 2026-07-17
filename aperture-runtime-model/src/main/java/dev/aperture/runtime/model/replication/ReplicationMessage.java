package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

/** Platform-neutral payload emitted by an authoritative runtime. */
public sealed interface ReplicationMessage permits ObjectSnapshotMessage, StateDeltaMessage,
	ObjectRemovedMessage, EventDeltaMessage {
	int protocolVersion();

	ArchitecturalObjectId objectId();
}
