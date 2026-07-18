package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.state.StateSchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Transport-neutral client-side projection of server-authoritative objects.
 * Gaps are never guessed: callers must request a fresh snapshot after RESYNC_REQUIRED.
 */
public final class ClientReplicaStore {
	private final int protocolVersion;
	private final StateSchemaResolver schemaResolver;
	private final Map<ArchitecturalObjectId, ReplicaObject> replicas = new LinkedHashMap<>();
	private final Map<ArchitecturalObjectId, ReplicaEventCursor> eventCursors = new LinkedHashMap<>();

	public ClientReplicaStore(int protocolVersion, StateSchemaResolver schemaResolver) {
		if (protocolVersion < 1) throw new IllegalArgumentException("protocolVersion must be >= 1");
		this.protocolVersion = protocolVersion;
		this.schemaResolver = Objects.requireNonNull(schemaResolver, "schemaResolver");
	}

	public ApplyResult apply(ReplicationMessage message) {
		Objects.requireNonNull(message, "message");
		if (message.protocolVersion() != protocolVersion) return result(Status.PROTOCOL_MISMATCH, message.objectId());
		if (message instanceof ObjectSnapshotMessage snapshot) return apply(snapshot);
		if (message instanceof StateDeltaMessage delta) return apply(delta);
		if (message instanceof EventDeltaMessage events) return apply(events);
		if (message instanceof ObjectRemovedMessage removed) return apply(removed);
		throw new IllegalArgumentException("Unsupported replication message: " + message.getClass().getName());
	}

	public Optional<ReplicaObject> replica(ArchitecturalObjectId objectId) {
		return Optional.ofNullable(replicas.get(Objects.requireNonNull(objectId, "objectId")));
	}

	public Optional<ReplicaEventCursor> eventCursor(ArchitecturalObjectId objectId) {
		return Optional.ofNullable(eventCursors.get(Objects.requireNonNull(objectId, "objectId")));
	}

	public Map<ArchitecturalObjectId, ReplicaObject> replicas() { return Map.copyOf(replicas); }

	private ApplyResult apply(ObjectSnapshotMessage message) {
		ReplicaObject current = replicas.get(message.objectId());
		if (current != null && message.snapshot().instance().revision() < current.instance().revision()) {
			return result(Status.ALREADY_APPLIED, message.objectId());
		}
		StateSchema schema = Objects.requireNonNull(schemaResolver.resolve(message.snapshot().instance()),
			"State schema resolver returned null");
		replicas.put(message.objectId(), message.snapshot().restore(schema));
		return result(Status.SNAPSHOT_APPLIED, message.objectId());
	}

	private ApplyResult apply(StateDeltaMessage message) {
		ReplicaObject current = replicas.get(message.objectId());
		if (current == null) return result(Status.RESYNC_REQUIRED, message.objectId());
		ReplicaObject.ApplyResult applied = current.apply(message);
		return switch (applied.status()) {
			case APPLIED -> {
				replicas.put(message.objectId(), applied.replica());
				yield result(Status.DELTA_APPLIED, message.objectId());
			}
			case ALREADY_APPLIED -> result(Status.ALREADY_APPLIED, message.objectId());
			case RESYNC_REQUIRED, OBJECT_MISMATCH -> result(Status.RESYNC_REQUIRED, message.objectId());
		};
	}

	private ApplyResult apply(EventDeltaMessage message) {
		if (!replicas.containsKey(message.objectId())) return result(Status.RESYNC_REQUIRED, message.objectId());
		ReplicaEventCursor current = eventCursors.computeIfAbsent(message.objectId(), id -> new ReplicaEventCursor(id, 0));
		ReplicaEventCursor.ApplyResult applied = current.apply(message);
		return switch (applied.status()) {
			case APPLIED -> {
				eventCursors.put(message.objectId(), applied.cursor());
				yield result(Status.EVENTS_APPLIED, message.objectId());
			}
			case ALREADY_APPLIED -> result(Status.ALREADY_APPLIED, message.objectId());
			case RESYNC_REQUIRED, OBJECT_MISMATCH -> result(Status.RESYNC_REQUIRED, message.objectId());
		};
	}

	private ApplyResult apply(ObjectRemovedMessage message) {
		ReplicaObject current = replicas.get(message.objectId());
		if (current == null) return result(Status.ALREADY_APPLIED, message.objectId());
		if (message.finalObjectRevision() < current.instance().revision()) {
			return result(Status.ALREADY_APPLIED, message.objectId());
		}
		replicas.remove(message.objectId());
		eventCursors.remove(message.objectId());
		return result(Status.OBJECT_REMOVED, message.objectId());
	}

	private ApplyResult result(Status status, ArchitecturalObjectId objectId) {
		return new ApplyResult(status, objectId, replica(objectId));
	}

	public record ApplyResult(Status status, ArchitecturalObjectId objectId, Optional<ReplicaObject> replica) {
		public ApplyResult {
			Objects.requireNonNull(status, "status");
			Objects.requireNonNull(objectId, "objectId");
			Objects.requireNonNull(replica, "replica");
		}
	}

	public enum Status {
		SNAPSHOT_APPLIED, DELTA_APPLIED, EVENTS_APPLIED, OBJECT_REMOVED,
		ALREADY_APPLIED, RESYNC_REQUIRED, PROTOCOL_MISMATCH
	}
}
