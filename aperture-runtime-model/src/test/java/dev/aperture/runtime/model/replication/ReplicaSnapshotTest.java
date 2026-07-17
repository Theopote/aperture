package dev.aperture.runtime.model.replication;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.state.RuntimeState;
import dev.aperture.runtime.model.state.StateDistribution;
import dev.aperture.runtime.model.state.StatePersistence;
import dev.aperture.runtime.model.state.StatePropertyDefinition;
import dev.aperture.runtime.model.state.StateSchema;
import dev.aperture.runtime.model.state.StateValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReplicaSnapshotTest {
	@Test
	void fullSynchronizationDoesNotExposeServerOnlyPersistentState() {
		StateSchema schema = StateSchema.builder("aperture:secured_door", 1)
			.property("openRatio", StatePropertyDefinition.number(0, 0.0, 1.0,
				StatePersistence.PERSISTENT, StateDistribution.REPLICATED))
			.property("accessToken", StatePropertyDefinition.bool(false,
				StatePersistence.PERSISTENT, StateDistribution.SERVER_ONLY))
			.build();
		RuntimeState serverState = new RuntimeState(schema, Map.of(
			"openRatio", StateValue.number(0.5), "accessToken", StateValue.bool(true)),
			dev.aperture.runtime.model.state.StateRevision.INITIAL, java.time.Instant.EPOCH);
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(1,
			ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:secured_door"),
			new ArchitecturalFamilyId("aperture:opening"), ParameterSet.empty(), Transform3d.identity(),
			List.of(), Map.of("accessToken", StateValue.bool(true)), 0, Map.of());

		ReplicaSnapshot snapshot = ReplicaSnapshot.capture(instance, serverState);
		assertEquals(Map.of("openRatio", StateValue.number(0.5)), snapshot.distributedState());
		assertFalse(snapshot.instance().persistentState().containsKey("accessToken"));
		assertEquals(StateValue.bool(false), snapshot.restore(schema).state().value("accessToken"));
	}
}
