package dev.aperture.core.serialization;

import com.google.gson.JsonParser;
import dev.aperture.core.component.ComponentKind;
import dev.aperture.core.component.DividerComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentAssemblyReaderTest {
	private final ComponentAssemblyReader reader = new ComponentAssemblyReader();

	@Test
	void readsTypedComponentArray() {
		var assembly = reader.read(JsonParser.parseString("""
			[
			  { "kind": "frame", "id": "grid_frame", "profile": "aperture:frame_l_50x80" },
			  { "kind": "divider", "id": "vertical_mullions", "source": "parameter:cols" },
			  { "kind": "glass", "id": "unit_glazing", "system": "aperture:single_glazed" }
			]
			"""));

		assertEquals(3, assembly.size());
		assertEquals("grid_frame", assembly.frame().orElseThrow().ref().id());
		assertEquals("unit_glazing", assembly.glass().orElseThrow().ref().id());
		assertTrue(assembly.ofKind(ComponentKind.DIVIDER).getFirst() instanceof DividerComponent);
	}

	@Test
	void rejectsLegacyObjectMapFormat() {
		assertThrows(IllegalArgumentException.class, () -> reader.read(JsonParser.parseString("""
			{
			  "frame": { "profile": "aperture:frame_l_50x80" },
			  "glazing": { "system": "aperture:single_glazed" }
			}
			""")));
	}
}
