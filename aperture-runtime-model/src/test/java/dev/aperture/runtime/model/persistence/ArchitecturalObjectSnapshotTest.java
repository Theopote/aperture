package dev.aperture.runtime.model.persistence;

import dev.aperture.math.Transform3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.behavior.BehaviorId;
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

class ArchitecturalObjectSnapshotTest {
	@Test
	void savesPersistentSubsetAndReconstructsTransientDefaults() {
		StateSchema schema = StateSchema.builder("test:door", 1)
			.property("openRatio", StatePropertyDefinition.number(0, 0.0, 1.0,
				StatePersistence.PERSISTENT, StateDistribution.REPLICATED))
			.property("motion", StatePropertyDefinition.enumeration("idle", java.util.Set.of("idle", "opening"),
				StatePersistence.TRANSIENT, StateDistribution.REPLICATED))
			.build();
		RuntimeState state = RuntimeState.initial(schema).apply(new StatePatch(
			StateRevision.INITIAL,
			Map.of("openRatio", StateValue.number(0.5), "motion", StateValue.enumeration("opening")),
			Instant.parse("2026-07-17T12:00:00Z")));
		ArchitecturalObjectId id = ArchitecturalObjectId.random();
		ArchitecturalObjectInstance instance = new ArchitecturalObjectInstance(
			1, id, ArchitecturalTypeId.parse("test:door"), new ArchitecturalFamilyId("test:opening"),
			ParameterSet.empty(), Transform3d.identity(), List.of(), Map.of(), 9, Map.of());
		BehaviorConfiguration behavior = new BehaviorConfiguration(
			new BehaviorId("test:auto_close"), 1, Map.of("delaySeconds", 3));

		ArchitecturalObjectSnapshot snapshot = ArchitecturalObjectSnapshot.capture(instance, state, List.of(behavior));
		assertEquals(Map.of("openRatio", StateValue.number(0.5)), snapshot.persistentState());
		assertFalse(snapshot.instance().persistentState().containsKey("motion"));

		InMemoryArchitecturalObjectPersistence store = new InMemoryArchitecturalObjectPersistence();
		store.save(snapshot);
		ArchitecturalObjectSnapshot loaded = store.load(id).orElseThrow();
		RuntimeState restored = loaded.restoreState(schema);
		assertEquals(StateValue.number(0.5), restored.value("openRatio"));
		assertEquals(StateValue.enumeration("idle"), restored.value("motion"));
		assertEquals(behavior, loaded.behaviors().getFirst());
		assertEquals(9, loaded.instance().revision());
	}
}
