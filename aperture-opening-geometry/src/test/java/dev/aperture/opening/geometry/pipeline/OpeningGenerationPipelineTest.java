package dev.aperture.opening.geometry.pipeline;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.opening.geometry.generator.GenerationTestSupport;
import dev.aperture.opening.component.ComponentPlanBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningGenerationPipelineTest {
	@Test
	void componentPipelineRunsStepsInPlannedOrderForFixedWindow() {
		var assembly = BuiltinOpeningTypes.fixedWindow().components();
		assertEquals(
			ComponentPlanBuilder.plannedStepIds(assembly),
			ComponentPlanBuilder.buildPlan(assembly).stepIds()
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
	void doorPipelineBuildsMultiPanelAndSillGeometry() {
		var result = GenerationTestSupport.generateDoorPipeline(ParameterSet.empty());

		assertTrue(result.meshes().partsByPath().containsKey("door_leaf.0.bottom"));
		assertTrue(result.meshes().partsByPath().containsKey("door_leaf.1.bottom"));
		assertTrue(result.meshes().partsByPath().containsKey("door_leaf.0.infill"));
		assertTrue(result.meshes().partsByPath().containsKey("door_leaf.0.glazing"));
		assertTrue(result.meshes().partsByPath().containsKey("threshold.main"));
		assertTrue(result.meshes().partsByPath().containsKey("handle.main"));
	}
}
