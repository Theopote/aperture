package dev.aperture.runtime.model.replication;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.state.StateRevision;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
/** Authority acknowledgement for an accepted and committed command. */
public record CommandAcceptedMessage(int protocolVersion, ArchitecturalObjectId objectId, UUID commandId, long resultingObjectRevision, StateRevision resultingStateRevision, Instant timestamp) implements ReplicationMessage {
 public CommandAcceptedMessage { if(protocolVersion<1||resultingObjectRevision<0) throw new IllegalArgumentException("invalid revision"); Objects.requireNonNull(objectId); Objects.requireNonNull(commandId); Objects.requireNonNull(resultingStateRevision); Objects.requireNonNull(timestamp); }
}
