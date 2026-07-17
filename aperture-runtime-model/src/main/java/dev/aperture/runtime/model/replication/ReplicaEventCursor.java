package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;

import java.util.Objects;

/** Tracks ordered event delivery for one object independently from state revision. */
public record ReplicaEventCursor(ArchitecturalObjectId objectId, long sequence) {
	public ReplicaEventCursor {
		Objects.requireNonNull(objectId, "objectId");
		if (sequence < 0) throw new IllegalArgumentException("sequence must be non-negative");
	}

	public ApplyResult apply(EventDeltaMessage message) {
		if (!objectId.equals(message.objectId())) {
			return new ApplyResult(ApplyResult.Status.OBJECT_MISMATCH, this);
		}
		if (message.resultingSequence() <= sequence) {
			return new ApplyResult(ApplyResult.Status.ALREADY_APPLIED, this);
		}
		if (message.baseSequence() != sequence) {
			return new ApplyResult(ApplyResult.Status.RESYNC_REQUIRED, this);
		}
		return new ApplyResult(ApplyResult.Status.APPLIED,
			new ReplicaEventCursor(objectId, message.resultingSequence()));
	}

	public record ApplyResult(Status status, ReplicaEventCursor cursor) {
		public enum Status { APPLIED, ALREADY_APPLIED, RESYNC_REQUIRED, OBJECT_MISMATCH }
	}
}
