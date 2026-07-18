package dev.aperture.runtime.model.replication;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.state.StateRevision;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
/** Structured authority rejection. */
public record CommandRejectedMessage(int protocolVersion, ArchitecturalObjectId objectId, UUID commandId, ErrorCode errorCode, String message, long authoritativeObjectRevision, StateRevision authoritativeStateRevision, Instant timestamp) implements ReplicationMessage {
 public CommandRejectedMessage { if(protocolVersion<1||authoritativeObjectRevision < -1) throw new IllegalArgumentException("invalid revision"); Objects.requireNonNull(objectId); Objects.requireNonNull(commandId); Objects.requireNonNull(errorCode); Objects.requireNonNull(message); Objects.requireNonNull(authoritativeStateRevision); Objects.requireNonNull(timestamp); }
 public enum ErrorCode { OBJECT_NOT_FOUND, REVISION_CONFLICT, COMMAND_UNSUPPORTED, COMMAND_REJECTED, PROTOCOL_MISMATCH }
}
