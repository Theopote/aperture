package dev.aperture.core.serialization;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningInstanceCodecTest {
	private final OpeningInstanceCodec codec = new OpeningInstanceCodec();
	private final OpeningTypeDefinition definition = BuiltinOpeningTypes.fixedWindow();

	@Test
	void readsFixedWindowInstanceFromClasspathResource() throws Exception {
		try (InputStream stream = getClass().getClassLoader()
			.getResourceAsStream("opening_instances/fixed_window_wall.json")) {
			OpeningInstance instance = codec.read(stream, definition);

			assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), instance.instanceId());
			assertEquals(BuiltinOpeningTypes.FIXED_WINDOW_ID, instance.typeId());
			assertEquals(1200, instance.parameters().requireLength("width"), 0.001);
			assertEquals(1500, instance.parameters().requireLength("height"), 0.001);
			assertEquals(1, instance.parameters().requireCount("mullions"));
			assertEquals(100, instance.transform().origin().x(), 0.001);
			assertEquals(Facing.NORTH, instance.transform().facing());
			assertEquals(HostBinding.wall("section:0,0,0"), instance.host());
		}
	}

	@Test
	void roundTripsInstanceJson() {
		OpeningInstance original = OpeningInstance.builder(BuiltinOpeningTypes.FIXED_WINDOW_ID)
			.instanceId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
			.parameters(ParameterSet.builder()
				.put("width", ParameterValue.length(900))
				.put("height", ParameterValue.length(1200))
				.put("mullions", ParameterValue.count(2))
				.put("frame_width", ParameterValue.length(40))
				.build())
			.transform(Transform3d.at(50, 75, 10, Facing.EAST))
			.host(HostBinding.wall("section:1,0,0"))
			.revision(3)
			.build();

		String json = codec.toJson(original);
		OpeningInstance restored = codec.fromJson(json, definition);

		assertEquals(original, restored);
		assertTrue(json.contains("\"typeId\": \"aperture:fixed_window\""));
	}
}
