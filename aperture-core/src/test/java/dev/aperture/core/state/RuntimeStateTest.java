package dev.aperture.core.state;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.core.instance.OpeningStateSchemas;
import dev.aperture.core.serialization.OpeningInstanceCodec;
import dev.aperture.core.serialization.OpeningTypeDefinitionReader;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeStateTest {
	@Test
	void separatesPersistentAndTransientPropertiesAndAdvancesStateRevision() {
		RuntimeState initial = RuntimeState.initial(OpeningStateSchemas.OPERABLE);
		Instant timestamp = Instant.parse("2026-07-17T05:00:00Z");

		RuntimeState moved = initial.transition(
			Map.of("openRatio", 1.0, "locked", true),
			Map.of("motion", "opening", "activeInteractor", "player:alice"),
			timestamp
		);

		assertEquals(1.0, moved.number("openRatio"));
		assertEquals(true, moved.bool("locked"));
		assertEquals("opening", moved.text("motion"));
		assertEquals(1, moved.revision());
		assertEquals(timestamp, moved.timestamp());
	}

	@Test
	void schemaRejectsUnknownOutOfRangeAndWrongPartitionValues() {
		RuntimeState state = RuntimeState.initial(OpeningStateSchemas.OPERABLE);

		assertThrows(IllegalArgumentException.class, () -> state.transition(Map.of("missing", 1), Map.of(), Instant.EPOCH));
		assertThrows(IllegalArgumentException.class, () -> state.transition(Map.of("openRatio", 2.0), Map.of(), Instant.EPOCH));
		assertThrows(IllegalArgumentException.class, () -> state.transition(Map.of("motion", "opening"), Map.of(), Instant.EPOCH));
	}

	@Test
	void definitionDeclaresSchemaAndInstanceJsonPersistsOnlyPersistentState() throws Exception {
		var definition = new OpeningTypeDefinitionReader().read(
			RuntimeStateTest.class.getClassLoader().getResourceAsStream("opening_types/door.json")
		);
		OpeningState state = new OpeningState(RuntimeState.initial(definition.stateSchema())).transition(
			Map.of("openRatio", 1.0, "locked", true),
			Map.of("motion", "opening", "activeInteractor", "player:alice"),
			Instant.parse("2026-07-17T05:00:00Z")
		);
		OpeningInstance instance = OpeningInstance.builder(BuiltinOpeningTypes.DOOR_ID).state(state).build();
		OpeningInstanceCodec codec = new OpeningInstanceCodec();

		String json = codec.toJson(instance);
		OpeningInstance restored = codec.fromJson(json, definition);

		assertFalse(json.contains("activeInteractor"));
		assertFalse(json.contains("motion"));
		assertEquals(1.0, restored.state().openRatio());
		assertEquals(true, restored.state().locked());
		assertEquals("idle", restored.state().runtimeState().text("motion"));
		assertEquals(1, restored.state().runtimeState().revision());
	}
}
