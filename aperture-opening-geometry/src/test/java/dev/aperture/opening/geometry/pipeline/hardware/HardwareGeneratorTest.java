package dev.aperture.opening.geometry.pipeline.hardware;

import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.opening.geometry.generator.GenerationTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HardwareGeneratorTest {
	@Test
	void doorGeneratesHingesAndHandleFromSeparateComponents() {
		var result = GenerationTestSupport.generateDoorPipeline(ParameterSet.empty());

		assertTrue(result.meshes().partsByPath().containsKey("hinges.hinge.1"));
		assertTrue(result.meshes().partsByPath().containsKey("hinges.hinge.2"));
		assertTrue(result.meshes().partsByPath().containsKey("hinges.hinge.3"));
		assertTrue(result.meshes().partsByPath().containsKey("handle.main"));
	}
}
