package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateValue;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** One contiguous authoritative state transition; geometry and meshes are never carried. */
public record StateDeltaMessage(
	int protocolVersion,
	ArchitecturalObjectId objectId,
	long baseObjectRevision,
	long resultingObjectRevision,
	StateRevision baseStateRevision,
	StateRevision resultingStateRevision,
	Map<String, StateValue> updates,
	Instant timestamp
) implements ReplicationMessage {
	public StateDeltaMessage {
		if (protocolVersion < 1) throw new IllegalArgumentException("protocolVersion must be >= 1");
		Objects.requireNonNull(objectId, "objectId");
		if (baseObjectRevision < 0 || resultingObjectRevision != baseObjectRevision + 1) {
			throw new IllegalArgumentException("Object revisions must describe one contiguous transition");
		}
		Objects.requireNonNull(baseStateRevision, "baseStateRevision");
		Objects.requireNonNull(resultingStateRevision, "resultingStateRevision");
		if (resultingStateRevision.value() != baseStateRevision.value() + 1) {
			throw new IllegalArgumentException("State revisions must describe one contiguous transition");
		}
		updates = Map.copyOf(updates);
		Objects.requireNonNull(timestamp, "timestamp");
	}
}
