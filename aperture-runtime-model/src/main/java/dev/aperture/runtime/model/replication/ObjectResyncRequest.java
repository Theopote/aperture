package dev.aperture.runtime.model.replication;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.state.StateRevision;
import java.util.Objects;
/** Requests a full snapshot after a client detects a missing or invalid delta. */
public record ObjectResyncRequest(int protocolVersion, ArchitecturalObjectId objectId, long observedObjectRevision, StateRevision observedStateRevision, long observedEventSequence, String reason) implements ReplicationMessage {
 public ObjectResyncRequest { if(protocolVersion<1||observedObjectRevision < -1||observedEventSequence<0) throw new IllegalArgumentException("invalid cursor"); Objects.requireNonNull(objectId); Objects.requireNonNull(observedStateRevision); Objects.requireNonNull(reason); }
}
