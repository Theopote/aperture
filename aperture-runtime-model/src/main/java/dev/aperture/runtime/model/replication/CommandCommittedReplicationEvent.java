package dev.aperture.runtime.model.replication;

import java.util.Objects;
import java.util.UUID;

/** Public acknowledgement of a server-accepted command. */
public record CommandCommittedReplicationEvent(UUID commandId, long resultingObjectRevision)
	implements ReplicatedEvent {
	public CommandCommittedReplicationEvent {
		Objects.requireNonNull(commandId, "commandId");
		if (resultingObjectRevision < 0) {
			throw new IllegalArgumentException("resultingObjectRevision must be non-negative");
		}
	}

	@Override
	public String eventType() { return "command_committed"; }
}
