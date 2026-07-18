package dev.aperture.runtime.model.replication;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.state.StateDistribution;
import dev.aperture.runtime.model.state.StatePatch;
import dev.aperture.runtime.model.state.StatePersistence;
import dev.aperture.runtime.model.state.StatePropertyDefinition;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateSchema;
import dev.aperture.runtime.model.state.StateValue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientReplicaStoreTest {
	private static final Instant TICK = Instant.parse("2026-07-18T12:00:00Z");

	@Test
	void twoClientsConvergeFromTheSameAuthoritativeMessages() {
		Fixture fixture = fixture();
		ClientReplicaStore first = new ClientReplicaStore(1, ignored -> fixture.schema);
		ClientReplicaStore second = new ClientReplicaStore(1, ignored -> fixture.schema);
		ObjectSnapshotMessage snapshot = new ObjectSnapshotMessage(1,
			ReplicaSnapshot.capture(fixture.instance, fixture.initial));
		RuntimeState opened = fixture.initial.apply(new StatePatch(StateRevision.INITIAL,
			Map.of("openRatio", StateValue.number(1)), TICK));
		StateDeltaMessage delta = StateDeltaFactory.between(
			fixture.instance, fixture.initial, fixture.instance.withRevision(1), opened);

		for (ClientReplicaStore client : List.of(first, second)) {
			assertEquals(ClientReplicaStore.Status.SNAPSHOT_APPLIED, client.apply(snapshot).status());
			assertEquals(ClientReplicaStore.Status.DELTA_APPLIED, client.apply(delta).status());
		}

		assertEquals(first.replicas(), second.replicas());
		assertEquals(StateValue.number(1), first.replica(fixture.instance.objectId())
			.orElseThrow().state().value("openRatio"));
	}

	@Test
	void revisionGapRequiresSnapshotAndSnapshotRepairsReplica() {
		Fixture fixture = fixture();
		ClientReplicaStore client = new ClientReplicaStore(1, ignored -> fixture.schema);
		client.apply(new ObjectSnapshotMessage(1, ReplicaSnapshot.capture(fixture.instance, fixture.initial)));
		StateDeltaMessage future = new StateDeltaMessage(1, fixture.instance.objectId(), 1, 2,
			new StateRevision(1), new StateRevision(2), Map.of("openRatio", StateValue.number(1)), TICK);

		assertEquals(ClientReplicaStore.Status.RESYNC_REQUIRED, client.apply(future).status());

		RuntimeState authoritative = new RuntimeState(fixture.schema,
			Map.of("openRatio", StateValue.number(1)), new StateRevision(2), TICK);
		ArchitecturalObjectInstance revisionTwo = fixture.instance.withRevision(2);
		assertEquals(ClientReplicaStore.Status.SNAPSHOT_APPLIED, client.apply(new ObjectSnapshotMessage(
			1, ReplicaSnapshot.capture(revisionTwo, authoritative))).status());
		assertEquals(2, client.replica(fixture.instance.objectId()).orElseThrow().instance().revision());
	}

	@Test
	void authoritativeRemovalClearsStateAndEventCursor() {
		Fixture fixture = fixture();
		ClientReplicaStore client = new ClientReplicaStore(1, ignored -> fixture.schema);
		client.apply(new ObjectSnapshotMessage(1, ReplicaSnapshot.capture(fixture.instance, fixture.initial)));
		client.apply(new EventDeltaMessage(1, fixture.instance.objectId(), 0, 0, List.of(), TICK));

		assertEquals(ClientReplicaStore.Status.OBJECT_REMOVED, client.apply(new ObjectRemovedMessage(
			1, fixture.instance.objectId(), 0, TICK)).status());
		assertTrue(client.replica(fixture.instance.objectId()).isEmpty());
		assertTrue(client.eventCursor(fixture.instance.objectId()).isEmpty());
	}

	private static Fixture fixture() {
		StateSchema schema = StateSchema.builder("aperture:door", 1)
			.property("openRatio", StatePropertyDefinition.number(0, 0.0, 1.0,
				StatePersistence.PERSISTENT, StateDistribution.REPLICATED))
			.build();
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(1,
			ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"), ParameterSet.empty(), Transform3d.identity(),
			List.of(), Map.of(), 0, Map.of());
		return new Fixture(schema, instance, RuntimeState.initial(schema));
	}

	private record Fixture(StateSchema schema, ArchitecturalObjectInstance instance, RuntimeState initial) { }
}

