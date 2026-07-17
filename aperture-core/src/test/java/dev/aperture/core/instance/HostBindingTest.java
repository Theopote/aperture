package dev.aperture.core.instance;

import com.google.gson.JsonParser;
import dev.aperture.core.object.ArchitecturalObjectId;
import dev.aperture.core.serialization.HostBindingJson;
import dev.aperture.math.LocalFrame;
import dev.aperture.math.Vec3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class HostBindingTest {
	@Test void structuredBindingRoundTripsDependencyData() {
		HostBinding binding = new HostBinding(
			new ArchitecturalObjectId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
			HostFeatureId.face("exterior-face:west"),
			LocalFrame.fromSurfaceNormal(new Vec3d(100, 200, 300), new Vec3d(0, 0, -1)),
			HostAttachmentMode.CUT_THROUGH,
			ParameterSet.builder().put("revealDepth", ParameterValue.length(125))
				.put("structuralLayer", ParameterValue.enumValue("core")).build(),
			42L
		);
		var json = HostBindingJson.write(binding);
		HostBinding restored = HostBindingJson.read(json);
		assertEquals(binding, restored);
		assertEquals(new Vec3d(0, 0, 1), restored.insertionFrame().interiorDirection());
		assertEquals(125, restored.attachmentParameters().requireLength("revealDepth"), 0.001);
		assertTrue(json.has("hostId"));
		assertFalse(json.has("anchor"));
	}

	@Test void migratesLegacyAnchorToStableIdentity() {
		HostBinding expected = HostBinding.wall("section:0,0,0");
		HostBinding migrated = HostBindingJson.read(JsonParser.parseString(
			"{\"type\":\"wall\",\"anchor\":\"section:0,0,0\"}").getAsJsonObject());
		assertEquals(expected, migrated);
		assertTrue(expected.sameHostFeature(migrated));
		assertFalse(expected.hostId().isNone());
	}

	@Test void rejectsInvalidDependencyAndFrame() {
		assertThrows(IllegalArgumentException.class, () -> new HostBinding(
			ArchitecturalObjectId.NONE, HostFeatureId.namedAnchor("invalid"), LocalFrame.identity(),
			HostAttachmentMode.SURFACE_MOUNTED, ParameterSet.empty(), -1));
		assertThrows(IllegalArgumentException.class, () -> new LocalFrame(Vec3d.ZERO,
			new Vec3d(1, 0, 0), new Vec3d(1, 0, 0), new Vec3d(0, 0, 1)));
	}
}
