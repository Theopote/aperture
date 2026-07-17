package dev.aperture.runtime.pipeline;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.instance.InMemoryOpeningInstanceStore;
import dev.aperture.core.instance.OpeningInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimePipelineTest {
	private InMemoryOpeningInstanceStore store;
	private RuntimePipeline pipeline;

	@BeforeEach
	void setUp() {
		OpeningTypeRegistry definitions = new OpeningTypeRegistry();
		BuiltinOpeningTypes.referenceDefinitions().forEach(definitions::register);
		store = new InMemoryOpeningInstanceStore();
		pipeline = new RuntimePipeline(
			List.of(new OpeningRuntimeBehavior(definitions)),
			new OpeningInstanceRepository(store)
		);
	}

	@Test
	void doorInteractionCommitsStateAndEmitsWorldPersistenceAndReplicationEffects() {
		OpeningInstance door = OpeningInstance.builder(BuiltinOpeningTypes.DOOR_ID).build();

		RuntimeResult result = pipeline.process(door, RuntimeInteraction.of("aperture:toggle"));

		OpeningInstance updated = assertInstanceOf(OpeningInstance.class, result.current());
		assertTrue(result.changed());
		assertEquals(1.0, updated.state().openRatio());
		assertEquals(door.revision() + 1, updated.revision());
		assertEquals(updated, store.findById(door.instanceId()).orElseThrow());
		assertEquals(3, result.effects().size());
		assertInstanceOf(RuntimeEffect.GeometryInvalidated.class, result.effects().get(0));
		assertInstanceOf(RuntimeEffect.PersistenceRequested.class, result.effects().get(1));
		assertInstanceOf(RuntimeEffect.ReplicationRequested.class, result.effects().get(2));
		assertEquals(
			java.util.Set.of(
				OpeningRuntimeBehavior.OPEN,
				OpeningRuntimeBehavior.CLOSE,
				OpeningRuntimeBehavior.TOGGLE
			),
			result.capabilities()
		);
	}

	@Test
	void idempotentInteractionDoesNotAdvanceRevisionOrEmitEffects() {
		OpeningInstance door = OpeningInstance.builder(BuiltinOpeningTypes.DOOR_ID).build();

		RuntimeResult result = pipeline.process(door, RuntimeInteraction.of("aperture:close"));

		assertFalse(result.changed());
		assertEquals(door.revision(), result.current().revision());
		assertTrue(result.effects().isEmpty());
		assertTrue(store.findById(door.instanceId()).isEmpty());
	}

	@Test
	void fixedWindowDoesNotPretendToHaveDoorCapabilities() {
		OpeningInstance window = OpeningInstance.builder(BuiltinOpeningTypes.FIXED_WINDOW_ID).build();

		assertThrows(
			IllegalArgumentException.class,
			() -> pipeline.process(window, RuntimeInteraction.of("aperture:toggle"))
		);
	}
}
