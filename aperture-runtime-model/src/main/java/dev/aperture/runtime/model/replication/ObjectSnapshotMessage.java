package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.Objects;

/** Complete network-safe authoritative state used for synchronization and recovery. */
public record ObjectSnapshotMessage(int protocolVersion, ReplicaSnapshot snapshot)
	implements ReplicationMessage {
	public ObjectSnapshotMessage {
		if (protocolVersion < 1) throw new IllegalArgumentException("protocolVersion must be >= 1");
		Objects.requireNonNull(snapshot, "snapshot");
	}

	@Override
	public ArchitecturalObjectId objectId() {
		return snapshot.instance().objectId();
	}
}
