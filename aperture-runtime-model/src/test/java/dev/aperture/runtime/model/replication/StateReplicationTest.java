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
import static org.junit.jupiter.api.Assertions.assertFalse;

class StateReplicationTest {
	private static final Instant TICK = Instant.parse("2026-07-17T12:00:00Z");

	@Test
	void publishesOnlyNetworkVisibleProperties() {
		Fixture fixture = fixture();
		RuntimeState current = fixture.state.apply(new StatePatch(StateRevision.INITIAL, Map.of(
			"openRatio", StateValue.number(0.75),
			"prediction", StateValue.number(0.8),
			"serverSecret", StateValue.bool(true),
			"localAnimation", StateValue.number(0.3)
		), TICK));
		StateDeltaMessage delta = StateDeltaFactory.between(
			fixture.instance, fixture.state, fixture.instance.withRevision(1), current);

		assertEquals(Map.of("openRatio", StateValue.number(0.75), "prediction", StateValue.number(0.8)),
			delta.updates());
		assertFalse(delta.updates().containsKey("serverSecret"));
		assertFalse(delta.updates().containsKey("localAnimation"));
	}

	@Test
	void appliesOnceAndTreatsDuplicateAsIdempotent() {
		Fixture fixture = fixture();
		RuntimeState current = fixture.state.apply(new StatePatch(
			StateRevision.INITIAL, Map.of("openRatio", StateValue.number(1)), TICK));
		StateDeltaMessage delta = StateDeltaFactory.between(
			fixture.instance, fixture.state, fixture.instance.withRevision(1), current);
		ReplicaObject.ApplyResult applied = new ReplicaObject(fixture.instance, fixture.state).apply(delta);

		assertEquals(ReplicaObject.ApplyResult.Status.APPLIED, applied.status());
		assertEquals(StateValue.number(1), applied.replica().state().value("openRatio"));
		assertEquals(ReplicaObject.ApplyResult.Status.ALREADY_APPLIED, applied.replica().apply(delta).status());
	}

	@Test
	void requestsSnapshotWhenARevisionGapIsObserved() {
		Fixture fixture = fixture();
		StateDeltaMessage future = new StateDeltaMessage(1, fixture.instance.objectId(), 1, 2,
			new StateRevision(1), new StateRevision(2), Map.of("openRatio", StateValue.number(1)), TICK);

		assertEquals(ReplicaObject.ApplyResult.Status.RESYNC_REQUIRED,
			new ReplicaObject(fixture.instance, fixture.state).apply(future).status());
	}

	private static Fixture fixture() {
		StateSchema schema = StateSchema.builder("aperture:door", 1)
			.property("openRatio", StatePropertyDefinition.number(0, 0.0, 1.0,
				StatePersistence.PERSISTENT, StateDistribution.REPLICATED))
			.property("prediction", StatePropertyDefinition.number(0, 0.0, 1.0,
				StatePersistence.TRANSIENT, StateDistribution.CLIENT_PREDICTED))
			.property("serverSecret", StatePropertyDefinition.bool(false,
				StatePersistence.TRANSIENT, StateDistribution.SERVER_ONLY))
			.property("localAnimation", StatePropertyDefinition.number(0, 0.0, 1.0,
				StatePersistence.TRANSIENT, StateDistribution.LOCAL))
			.build();
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(1,
			ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"), ParameterSet.empty(), Transform3d.identity(),
			List.of(), Map.of(), 0, Map.of());
		return new Fixture(instance, RuntimeState.initial(schema));
	}

	private record Fixture(ArchitecturalObjectInstance instance, RuntimeState state) { }
}
