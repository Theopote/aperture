package dev.aperture.runtime.model.replication;

import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.state.StateRevision;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventReplicationTest {
	@Test
	void advancesAContiguousEventCursorAndIgnoresDuplicateBatch() {
		ArchitecturalObjectId objectId = ArchitecturalObjectId.random();
		EventDeltaMessage message = new EventDeltaMessage(1, objectId, 4, 6, List.of(
			new CommandCommittedReplicationEvent(UUID.randomUUID(), 8),
			new StateTransitionReplicationEvent(new StateRevision(2), new StateRevision(3), Set.of("openRatio"))
		), Instant.EPOCH);

		ReplicaEventCursor.ApplyResult applied = new ReplicaEventCursor(objectId, 4).apply(message);
		assertEquals(ReplicaEventCursor.ApplyResult.Status.APPLIED, applied.status());
		assertEquals(6, applied.cursor().sequence());
		assertEquals(ReplicaEventCursor.ApplyResult.Status.ALREADY_APPLIED,
			applied.cursor().apply(message).status());
	}

	@Test
	void rejectsObjectMismatchAndRequestsResynchronizationForAGap() {
		ArchitecturalObjectId objectId = ArchitecturalObjectId.random();
		EventDeltaMessage message = new EventDeltaMessage(1, objectId, 4, 5,
			List.of(new CommandCommittedReplicationEvent(UUID.randomUUID(), 8)), Instant.EPOCH);
		assertEquals(ReplicaEventCursor.ApplyResult.Status.RESYNC_REQUIRED,
			new ReplicaEventCursor(objectId, 2).apply(message).status());
		assertEquals(ReplicaEventCursor.ApplyResult.Status.OBJECT_MISMATCH,
			new ReplicaEventCursor(ArchitecturalObjectId.random(), 4).apply(message).status());
	}

	@Test
	void rejectsSequenceRangesThatDoNotMatchPayloadCount() {
		assertThrows(IllegalArgumentException.class, () -> new EventDeltaMessage(
			1, ArchitecturalObjectId.random(), 0, 2,
			List.of(new CommandCommittedReplicationEvent(UUID.randomUUID(), 1)), Instant.EPOCH));
	}
}
