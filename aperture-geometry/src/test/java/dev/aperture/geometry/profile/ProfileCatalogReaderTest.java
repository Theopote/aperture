package dev.aperture.geometry.profile;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileCatalogReaderTest {
	private final ProfileCatalogReader reader = new ProfileCatalogReader();

	@Test
	void readsLProfileFromJson() throws Exception {
		String json = """
			{
			  "schemaVersion": 1,
			  "id": "aperture:frame_l_50x80",
			  "name": "L-Frame 50×80",
			  "points": [
			    { "u": 0, "v": 0 },
			    { "u": 50, "v": 0 },
			    { "u": 50, "v": 50 },
			    { "u": 30, "v": 50 },
			    { "u": 30, "v": 80 },
			    { "u": 0, "v": 80 }
			  ]
			}
			""";

		ProfileDefinition definition = reader.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));

		assertEquals("aperture:frame_l_50x80", definition.id());
		assertEquals(50, definition.bounds().width());
		assertEquals(80, definition.bounds().depth());
		assertEquals(6, definition.curve().segmentCount());
	}
}
