package dev.aperture.opening.geometry.generator;

import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.mesh.ShapeMesher;
import dev.aperture.geometry.model.GeometrySolid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningGeometryCompilerTest {
	@Test
	void generatesExtrudedFrameRailsAndGlazing() {
		var result = GenerationTestSupport.generateFixedWindow(ParameterSet.builder()
			.put("mullions", ParameterValue.count(2))
			.build());

		assertEquals(7, result.solids().size());
		assertEquals(1200, result.bounds().width());
		assertEquals(1500, result.bounds().height());
		assertEquals(80, result.bounds().depth());
	}

	@Test
	void frameRailsUseExtrusionShapesWithLProfile() {
		var result = GenerationTestSupport.generateFixedWindow();

		long frameRails = result.solids().stream()
			.filter(solid -> solid.componentPath().startsWith("frame.")
				&& !solid.componentPath().contains("mullion"))
			.count();

		assertEquals(4, frameRails);
	}

	@Test
	void goldenMeshBoundsForBottomRail() {
		var result = GenerationTestSupport.generateFixedWindow();

		GeometrySolid bottomRail = result.solids().stream()
			.filter(solid -> solid.componentPath().equals("frame.bottom"))
			.findFirst()
			.orElseThrow();

		Mesh mesh = ShapeMesher.mesh(bottomRail.shape(), bottomRail.localTransform());

		assertEquals(1200, bottomRail.bounds().width(), 0.01);
		assertEquals(50, bottomRail.bounds().height(), 0.01);
		assertEquals(80, bottomRail.bounds().depth(), 0.01);
		assertTrue(mesh.triangleCount() >= 8);
	}

	@Test
	void loadsCatalogProfilesFromClasspath() {
		var registry = GenerationTestSupport.profiles();

		assertEquals(2, registry.all().size());
		assertTrue(registry.findById("aperture:frame_l_50x80").isPresent());
	}
}
