package dev.aperture.runtime.persistence;

import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.behavior.BehaviorId;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.persistence.ArchitecturalObjectSnapshot;
import dev.aperture.runtime.model.persistence.BehaviorConfiguration;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateValue;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchitecturalObjectSnapshotJsonCodecTest {
	@Test
	void roundTripsDurableRuntimeObjectData() {
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(
			1, ArchitecturalObjectId.random(), ArchitecturalTypeId.parse("aperture:door"),
			new ArchitecturalFamilyId("aperture:opening"),
			ParameterSet.of("width", ParameterValue.length(900)), Transform3d.at(1, 2, 3, Facing.EAST),
			List.of(), Map.of("openRatio", StateValue.number(0.5)), 12, Map.of("source", "test"));
		ArchitecturalObjectSnapshot snapshot = new ArchitecturalObjectSnapshot(
			1, instance, Map.of("openRatio", StateValue.number(0.5)), new StateRevision(4),
			Instant.parse("2026-07-17T12:00:00Z"),
			List.of(new BehaviorConfiguration(new BehaviorId("aperture:auto_close"), 1, Map.of("delay", 3))));

		ArchitecturalObjectSnapshot restored = new ArchitecturalObjectSnapshotJsonCodec().decode(
			new ArchitecturalObjectSnapshotJsonCodec().encode(snapshot));
		assertEquals(snapshot.instance(), restored.instance());
		assertEquals(snapshot.persistentState(), restored.persistentState());
		assertEquals(snapshot.stateRevision(), restored.stateRevision());
		assertEquals(snapshot.behaviors().getFirst().behaviorId(), restored.behaviors().getFirst().behaviorId());
	}
}
