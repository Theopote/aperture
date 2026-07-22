package dev.aperture.editor.interaction;

import dev.aperture.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldUnitConverterTest {
	@Test void scalarConversionIsSymmetric() {
		assertEquals(1250.0, WorldUnitConverter.blocksToLength(1.25));
		assertEquals(1.25, WorldUnitConverter.lengthToBlocks(1250.0));
	}

	@Test void vectorConversionRoundTrips() {
		Vec3d blocks = new Vec3d(-2.5, 1.25, 0.125);
		assertEquals(blocks, WorldUnitConverter.worldLengthToBlocks(
			WorldUnitConverter.blocksToWorldLength(blocks)));
	}
}
