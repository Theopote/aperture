package dev.aperture.runtime.model.replication;
import dev.aperture.runtime.model.event.ActorRef;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.state.StateRevision;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
/** Untrusted client intent validated and decoded from an authority allow-list. */
public record CommandRequestMessage(int protocolVersion, ArchitecturalObjectId objectId, UUID commandId, String commandType, Map<String, String> payload, long expectedObjectRevision, StateRevision expectedStateRevision, ActorRef actor, Instant timestamp) implements ReplicationMessage {
 public CommandRequestMessage { if(protocolVersion<1) throw new IllegalArgumentException("protocolVersion must be >= 1"); Objects.requireNonNull(objectId); Objects.requireNonNull(commandId); if(commandType==null||commandType.isBlank()) throw new IllegalArgumentException("commandType is required"); payload=Map.copyOf(payload); if(expectedObjectRevision<0) throw new IllegalArgumentException("invalid revision"); Objects.requireNonNull(expectedStateRevision); Objects.requireNonNull(actor); Objects.requireNonNull(timestamp); }
}
