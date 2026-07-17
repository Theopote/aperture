package dev.aperture.runtime.model.state;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeStateTest {
	private static final StateSchema DOOR = StateSchema.builder("aperture:door", 1)
		.property("openRatio", StatePropertyDefinition.number(
			0, 0.0, 1.0, StatePersistence.PERSISTENT, StateDistribution.REPLICATED))
		.property("targetOpenRatio", StatePropertyDefinition.number(
			0, 0.0, 1.0, StatePersistence.TRANSIENT, StateDistribution.REPLICATED))
		.property("locked", StatePropertyDefinition.bool(
			false, StatePersistence.PERSISTENT, StateDistribution.REPLICATED))
		.property("enabled", StatePropertyDefinition.bool(
			true, StatePersistence.PERSISTENT, StateDistribution.REPLICATED))
		.property("motion", StatePropertyDefinition.enumeration(
			"idle", Set.of("idle", "opening", "closing", "blocked"),
			StatePersistence.TRANSIENT, StateDistribution.REPLICATED))
		.property("clearance", StatePropertyDefinition.number(
			0, 0.0, null, StatePersistence.DERIVED, StateDistribution.LOCAL))
		.build();

	@Test
	void appliesTypedPatchAndAdvancesRevision() {
		RuntimeState initial = RuntimeState.initial(DOOR);
		Instant time = Instant.parse("2026-07-17T10:15:30Z");
		RuntimeState opened = initial.apply(new StatePatch(
			StateRevision.INITIAL,
			Map.of(
				"targetOpenRatio", StateValue.number(1),
				"motion", StateValue.enumeration("opening")
			),
			time
		));

		assertEquals(new StateRevision(1), opened.revision());
		assertEquals(new StateValue.NumberValue(1), opened.value("targetOpenRatio"));
		assertEquals(time, opened.timestamp());
		assertFalse(opened.persistentValues().containsKey("motion"));
	}

	@Test
	void rejectsStaleInvalidAndDerivedPatches() {
		RuntimeState initial = RuntimeState.initial(DOOR);
		assertThrows(StateRevisionConflictException.class, () -> initial.apply(new StatePatch(
			new StateRevision(1), Map.of("locked", StateValue.bool(true)), Instant.EPOCH)));
		assertThrows(IllegalArgumentException.class, () -> initial.apply(new StatePatch(
			StateRevision.INITIAL, Map.of("openRatio", StateValue.number(2)), Instant.EPOCH)));
		assertThrows(IllegalArgumentException.class, () -> initial.apply(new StatePatch(
			StateRevision.INITIAL, Map.of("clearance", StateValue.number(1)), Instant.EPOCH)));
	}

	@Test
	void restoreUsesPersistentSubsetAndRebuildsTransientDefaults() {
		RuntimeState restored = RuntimeState.restore(
			DOOR,
			Map.of("openRatio", StateValue.number(0.5), "locked", StateValue.bool(true)),
			new StateRevision(7),
			Instant.parse("2026-07-17T00:00:00Z")
		);

		assertEquals(new StateValue.NumberValue(0.5), restored.value("openRatio"));
		assertEquals(new StateValue.NumberValue(0), restored.value("targetOpenRatio"));
		assertEquals(new StateValue.EnumValue("idle"), restored.value("motion"));
		assertEquals(new StateRevision(7), restored.revision());
	}
}
