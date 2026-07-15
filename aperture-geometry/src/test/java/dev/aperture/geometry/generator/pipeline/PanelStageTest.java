package dev.aperture.geometry.generator.pipeline;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.geometry.generator.GenerationTestSupport;
import dev.aperture.geometry.generator.RectangularWindowGenerator;
import dev.aperture.geometry.model.GeometryResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelStageTest {
	private static final RectangularWindowGenerator GENERATOR = new RectangularWindowGenerator();

	@Test
	void closedCasementGeneratesPanelSolidsWithoutFixedGlazing() {
		GeometryResult result = generateCasement(0);

		assertTrue(result.solids().stream().anyMatch(solid -> solid.componentPath().equals("panel.glazing")));
		assertFalse(result.solids().stream().anyMatch(solid -> solid.componentPath().equals("glazing")));
	}

	@Test
	void openPanelRotatesBoundsOutward() {
		GeometryResult closed = generateCasement(0);
		GeometryResult open = generateCasement(45);

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

	private static GeometryResult generateCasement(double openAngle) {
		GenerationContext context = GenerationTestSupport.context(
			BuiltinOpeningTypes.casementWindow(),
			ParameterSet.builder()
				.put("open_angle", ParameterValue.angle(openAngle))
				.put("frame_depth", ParameterValue.length(80))
				.build()
		);
		return GENERATOR.generate(context);
	}
}
