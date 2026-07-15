package dev.aperture.opening.geometry.pipeline;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.opening.geometry.generator.GenerationTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurtainWallPipelineTest {
	@Test
	void generatesGridGlazingAndDividers() {
		var result = GenerationTestSupport.generateCurtainWallPipeline(ParameterSet.empty());

		assertTrue(result.meshes().partsByPath().containsKey("glazing.0.0"));
		assertTrue(result.meshes().partsByPath().containsKey("glazing.2.3"));
		assertTrue(result.meshes().partsByPath().containsKey("divider.vertical.1"));
		assertTrue(result.meshes().partsByPath().containsKey("divider.horizontal.1"));
		assertTrue(result.meshes().partsByPath().containsKey("header.main"));
		assertTrue(result.meshes().partsByPath().containsKey("sill.main"));
	}

	@Test
	void gridCellCountMatchesColsAndRows() {
		var result = GenerationTestSupport.generateCurtainWallPipeline(ParameterSet.empty());
		long glazingCells = result.geometry().solids().stream()
			.filter(solid -> solid.componentPath().startsWith("glazing."))
			.count();
		assertEquals(12, glazingCells);
	}
}
