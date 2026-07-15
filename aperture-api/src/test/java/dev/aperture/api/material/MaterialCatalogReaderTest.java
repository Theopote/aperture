package dev.aperture.api.material;

import dev.aperture.render.material.BlendMode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MaterialCatalogReaderTest {
	private final MaterialCatalogReader reader = new MaterialCatalogReader();

	@Test
	void readsMaterialEntry() throws Exception {
		String json = """
			{
			  "id": "aperture:frame_oak",
			  "albedoTexture": "minecraft:block/oak_planks",
			  "roughness": 0.7,
			  "metalness": 0.0,
			  "blendMode": "opaque",
			  "doubleSided": false
			}
			""";

		MaterialCatalogEntry entry = reader.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

		assertEquals("aperture:frame_oak", entry.id());
		assertEquals("minecraft:block/oak_planks", entry.albedoTexture());
		assertEquals(0.7f, entry.roughness(), 0.001f);
		assertEquals(BlendMode.OPAQUE, entry.blendMode());
		assertFalse(entry.doubleSided());
	}

	@Test
	void readsSlotBindings() throws Exception {
		String json = """
			{
			  "frame": "aperture:frame_oak",
			  "glazing": "aperture:glazing_clear"
			}
			""";

		Map<String, String> bindings = reader.readSlotBindings(
			new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))
		);

		assertEquals("aperture:frame_oak", bindings.get("frame"));
		assertEquals("aperture:glazing_clear", bindings.get("glazing"));
	}
}
