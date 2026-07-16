package dev.aperture.core.serialization;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.component.ComponentKind;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.opening.OpeningCategory;
import dev.aperture.parameter.ParameterType;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningTypeDefinitionReaderTest {
	private final OpeningTypeDefinitionReader reader = new OpeningTypeDefinitionReader();

	@Test
	void readsFixedWindowFromClasspathResource() throws Exception {
		try (InputStream stream = getClass().getClassLoader()
			.getResourceAsStream("opening_types/fixed_window.json")) {
			OpeningTypeDefinition definition = reader.read(stream);

			assertEquals(BuiltinOpeningTypes.FIXED_WINDOW_ID, definition.id());
			assertEquals(OpeningCategory.WINDOW, definition.category());
			assertEquals("aperture:rectangular_window_v1", definition.generator().toString());
			assertTrue(definition.parametricSchema().get("width").isPresent());
			assertEquals(ParameterType.LENGTH, definition.parametricSchema().require("width").storageType());
			assertEquals(2, definition.materialSlots().size());
		}
	}

	@Test
	void readsFixedWindowComponentsFromArrayFormat() throws Exception {
		try (InputStream stream = getClass().getClassLoader()
			.getResourceAsStream("opening_types/fixed_window.json")) {
			OpeningTypeDefinition definition = reader.read(stream);

			assertTrue(definition.components().has(ComponentKind.FRAME));
			assertTrue(definition.components().has(ComponentKind.GLASS));
			assertTrue(definition.components().has(ComponentKind.MULLION));
			assertEquals("mullions", definition.components().mullion().orElseThrow().ref().id());
		}
	}
}
