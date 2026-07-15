package dev.aperture.opening.geometry.pipeline;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.opening.test.OpeningTestFixtures;
import dev.aperture.core.component.ComponentAssemblyPresets;
import dev.aperture.opening.component.ComponentPlanBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentPipelinePlannerTest {
	@Test
	void fixedWindowPlansOneStepPerComponentInstance() {
		assertEquals(
			List.of("frame", "glazing", "mullions"),
			ComponentPipelinePlanner.plannedStepIds(BuiltinOpeningTypes.fixedWindow().components())
		);
	}

	@Test
	void casementPipelineIncludesPanelStep() {
		var steps = ComponentPipelinePlanner.plannedStepIds(OpeningTestFixtures.casementWindow().components());
		assertTrue(steps.contains("panel"));
		assertTrue(steps.contains("glazing"));
	}

	@Test
	void doorPipelineUsesComponentIds() {
		var steps = ComponentPipelinePlanner.plannedStepIds(BuiltinOpeningTypes.door().components());
		assertEquals(
			List.of("door_frame", "door_leaf", "door_glass", "threshold", "hinges", "handle", "_mullions"),
			steps
		);
	}

	@Test
	void curtainWallPipelinePlansSeparateMullionInstances() {
		var steps = ComponentPipelinePlanner.plannedStepIds(ComponentAssemblyPresets.curtainWall(
			"aperture:frame_l_50x80",
			"aperture:single_glazed"
		));
		assertTrue(steps.contains("grid_frame"));
		assertTrue(steps.contains("vertical_mullions"));
		assertTrue(steps.contains("horizontal_mullions"));
		assertTrue(steps.contains("unit_glazing"));
		assertTrue(steps.contains("head"));
		assertTrue(steps.contains("sill"));
	}
}
