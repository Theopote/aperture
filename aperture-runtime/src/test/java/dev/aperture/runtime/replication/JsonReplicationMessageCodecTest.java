package dev.aperture.runtime.replication;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.replication.CommandCommittedReplicationEvent;
import dev.aperture.runtime.model.replication.EventDeltaMessage;
import dev.aperture.runtime.model.replication.ObjectRemovedMessage;
import dev.aperture.runtime.model.replication.ObjectSnapshotMessage;
import dev.aperture.runtime.model.replication.ReplicaSnapshot;
import dev.aperture.runtime.model.replication.ReplicationMessage;
import dev.aperture.runtime.model.replication.StateDeltaMessage;
import dev.aperture.runtime.model.replication.StateTransitionReplicationEvent;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateValue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonReplicationMessageCodecTest {
	private static final Instant TIME = Instant.parse("2026-07-17T12:00:00Z");

	@Test
	void roundTripsEveryAllowListedMessageKind() {
		ArchitecturalObjectId objectId = ArchitecturalObjectId.random();
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(1, objectId,
			ArchitecturalTypeId.parse("aperture:door"), new ArchitecturalFamilyId("aperture:opening"),
			ParameterSet.empty(), Transform3d.identity(), List.of(), Map.of(), 7, Map.of("source", "test"));
		List<ReplicationMessage> messages = List.of(
			new ObjectSnapshotMessage(1, new ReplicaSnapshot(instance,
				Map.of("openRatio", StateValue.number(0.5)), new StateRevision(3), TIME)),
			new StateDeltaMessage(1, objectId, 7, 8, new StateRevision(3), new StateRevision(4),
				Map.of("openRatio", StateValue.number(1)), TIME),
			new EventDeltaMessage(1, objectId, 10, 12, List.of(
				new CommandCommittedReplicationEvent(UUID.fromString("57bd45e5-bd55-4f45-bb25-98568062d895"), 8),
				new StateTransitionReplicationEvent(new StateRevision(3), new StateRevision(4),
					Set.of("openRatio"))), TIME),
			new ObjectRemovedMessage(1, objectId, 9, TIME)
		);

		JsonReplicationMessageCodec codec = new JsonReplicationMessageCodec();
		for (ReplicationMessage message : messages) assertEquals(message, codec.decode(codec.encode(message)));
	}

	@Test
	void rejectsUnknownMessageAndEventKinds() {
		JsonReplicationMessageCodec codec = new JsonReplicationMessageCodec();
		String objectId = ArchitecturalObjectId.random().toString();
		assertThrows(IllegalArgumentException.class, () -> codec.decode(
			"{\"protocolVersion\":1,\"objectId\":\"" + objectId + "\",\"kind\":\"private\"}"));
		assertThrows(IllegalArgumentException.class, () -> codec.decode(
			"{\"protocolVersion\":1,\"objectId\":\"" + objectId
				+ "\",\"kind\":\"event_delta\",\"baseSequence\":0,\"resultingSequence\":1,"
				+ "\"timestamp\":\"2026-07-17T12:00:00Z\",\"events\":[{\"type\":\"private\"}]}"));
	}
}
