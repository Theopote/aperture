package dev.aperture.geometry.pipeline.hardware;

import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.generator.GenerationTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HardwareGeneratorTest {
	@Test
	void doorGeneratesHingesAndHandle() {
		var result = GenerationTestSupport.generateDoorPipeline(ParameterSet.empty());

		assertTrue(result.meshes().partsByPath().containsKey("hardware.hinge.1"));
		assertTrue(result.meshes().partsByPath().containsKey("hardware.hinge.2"));
		assertTrue(result.meshes().partsByPath().containsKey("hardware.hinge.3"));
		assertTrue(result.meshes().partsByPath().containsKey("hardware.handle"));
	}
}
