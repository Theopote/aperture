package dev.aperture.geometry.pipeline;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.geometry.generator.GenerationTestSupport;
import dev.aperture.geometry.generator.RectangularWindowGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningPipelineTest {
	@Test
	void componentPipelineRunsStepsInPlannedOrderForFixedWindow() {
		var pipeline = new RectangularWindowGenerator().pipelineFor(
			GenerationTestSupport.context(BuiltinOpeningTypes.fixedWindow(), ParameterSet.empty())
		);

		assertEquals(
			ComponentPipelinePlanner.plannedStepIds(BuiltinOpeningTypes.fixedWindow().components()),
			pipeline.stepIds()
		);
	}

	@Test
	void pipelineProducesGeometryAndMeshAssembly() {
		var result = GenerationTestSupport.generateFixedWindowPipeline(ParameterSet.empty());

		assertFalse(result.geometry().solids().isEmpty());
		assertEquals(result.geometry().solids().size(), result.meshes().partsByPath().size());
		assertTrue(result.meshes().partsByPath().containsKey("frame.bottom"));
	}

	@Test
	void casementPipelineSkipsFixedGlazingAndBuildsPanelMeshes() {
		var result = GenerationTestSupport.generateCasementWindowPipeline(ParameterSet.builder()
			.put("open_angle", ParameterValue.angle(45))
			.put("frame_depth", ParameterValue.length(80))
			.build());

		assertFalse(result.geometry().solids().stream().anyMatch(s -> s.componentPath().equals("glazing")));
		assertTrue(result.meshes().partsByPath().containsKey("panel.glazing"));
	}

	@Test
	void doorPipelineBuildsPanelAndSillGeometry() {
		var result = GenerationTestSupport.generateDoorPipeline(ParameterSet.empty());

		assertTrue(result.meshes().partsByPath().containsKey("panel.bottom"));
		assertTrue(result.meshes().partsByPath().containsKey("sill.main"));
	}
}
