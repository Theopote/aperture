package dev.aperture.geometry.pipeline;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.component.ComponentAssemblyPresets;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentPipelinePlannerTest {
	@Test
	void fixedWindowPipelineIncludesFrameGlassAndDividerSteps() {
		assertEquals(
			List.of("profile", "frame", "glass", "accessory"),
			ComponentPipelinePlanner.plannedStepIds(BuiltinOpeningTypes.fixedWindow().components())
		);
	}

	@Test
	void casementPipelineIncludesPanelStep() {
		var steps = ComponentPipelinePlanner.plannedStepIds(BuiltinOpeningTypes.casementWindow().components());
		assertTrue(steps.contains("panel"));
		assertTrue(steps.contains("glass"));
	}

	@Test
	void doorPipelineIncludesHardwareAndSill() {
		var steps = ComponentPipelinePlanner.plannedStepIds(BuiltinOpeningTypes.door().components());
		assertEquals(List.of("profile", "frame", "panel", "glass", "sill", "hardware", "accessory"), steps);
	}

	@Test
	void curtainWallPipelineIncludesGridComponents() {
		var steps = ComponentPipelinePlanner.plannedStepIds(ComponentAssemblyPresets.curtainWall(
			"aperture:frame_l_50x80",
			"aperture:single_glazed"
		));
		assertTrue(steps.contains("header"));
		assertTrue(steps.contains("sill"));
		assertTrue(steps.contains("accessory"));
	}
}
