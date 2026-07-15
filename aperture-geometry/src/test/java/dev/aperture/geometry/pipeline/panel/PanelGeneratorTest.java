package dev.aperture.geometry.pipeline.panel;

import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.geometry.generator.GenerationTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelGeneratorTest {
	@Test
	void closedCasementGeneratesPanelSolidsWithoutFixedGlazing() {
		var result = GenerationTestSupport.generateCasementWindow(ParameterSet.builder()
			.put("open_angle", ParameterValue.angle(0))
			.put("frame_depth", ParameterValue.length(80))
			.build());

		assertTrue(result.solids().stream().anyMatch(solid -> solid.componentPath().equals("panel.glazing")));
		assertFalse(result.solids().stream().anyMatch(solid -> solid.componentPath().equals("glazing")));
	}

	@Test
	void openPanelRotatesBoundsOutward() {
		var closed = GenerationTestSupport.generateCasementWindow(ParameterSet.builder()
			.put("open_angle", ParameterValue.angle(0))
			.put("frame_depth", ParameterValue.length(80))
			.build());
		var open = GenerationTestSupport.generateCasementWindow(ParameterSet.builder()
			.put("open_angle", ParameterValue.angle(45))
			.put("frame_depth", ParameterValue.length(80))
			.build());

		var closedPanel = closed.solids().stream()
			.filter(solid -> solid.componentPath().equals("panel.glazing"))
			.findFirst()
			.orElseThrow();
		var openPanel = open.solids().stream()
			.filter(solid -> solid.componentPath().equals("panel.glazing"))
			.findFirst()
			.orElseThrow();

		assertTrue(openPanel.bounds().max().z() > closedPanel.bounds().max().z());
		assertTrue(openPanel.localTransform().hasRotation());
	}
}
