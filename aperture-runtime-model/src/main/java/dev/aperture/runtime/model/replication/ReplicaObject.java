package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.state.StatePatch;

import java.util.Objects;

/** Immutable client replica with deterministic delta acceptance rules. */
public record ReplicaObject(ArchitecturalObjectInstance instance, RuntimeState state) {
	public ReplicaObject {
		Objects.requireNonNull(instance, "instance");
		Objects.requireNonNull(state, "state");
	}

	public ApplyResult apply(StateDeltaMessage delta) {
		if (!instance.objectId().equals(delta.objectId())) return ApplyResult.objectMismatch(this);
		if (delta.resultingObjectRevision() <= instance.revision()
			&& delta.resultingStateRevision().value() <= state.revision().value()) {
			return ApplyResult.alreadyApplied(this);
		}
		if (delta.baseObjectRevision() != instance.revision()
			|| !delta.baseStateRevision().equals(state.revision())) {
			return ApplyResult.resyncRequired(this);
		}
		RuntimeState nextState = state.apply(new StatePatch(delta.baseStateRevision(), delta.updates(), delta.timestamp()));
		ArchitecturalObjectInstance nextInstance = instance.withRevision(delta.resultingObjectRevision());
		return ApplyResult.applied(new ReplicaObject(nextInstance, nextState));
	}

	public record ApplyResult(Status status, ReplicaObject replica) {
		public ApplyResult {
			Objects.requireNonNull(status, "status");
			Objects.requireNonNull(replica, "replica");
		}

		public static ApplyResult applied(ReplicaObject value) { return new ApplyResult(Status.APPLIED, value); }
		public static ApplyResult alreadyApplied(ReplicaObject value) { return new ApplyResult(Status.ALREADY_APPLIED, value); }
		public static ApplyResult resyncRequired(ReplicaObject value) { return new ApplyResult(Status.RESYNC_REQUIRED, value); }
		public static ApplyResult objectMismatch(ReplicaObject value) { return new ApplyResult(Status.OBJECT_MISMATCH, value); }

		public enum Status { APPLIED, ALREADY_APPLIED, RESYNC_REQUIRED, OBJECT_MISMATCH }
	}
}
