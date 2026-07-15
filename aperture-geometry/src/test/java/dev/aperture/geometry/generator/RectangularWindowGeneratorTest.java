package dev.aperture.geometry.generator;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.mesh.ShapeMesher;
import dev.aperture.geometry.model.GeometrySolid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RectangularWindowGeneratorTest {
	@Test
	void generatesExtrudedFrameRailsAndGlazing() {
		OpeningTypeDefinition definition = BuiltinOpeningTypes.fixedWindow();
		ParameterSet parameters = ParameterSet.mergeDefaults(definition.parameters(), ParameterSet.builder()
			.put("mullions", ParameterValue.count(2))
			.build());

		var result = new RectangularWindowGenerator().generate(definition, parameters);

		assertEquals(7, result.solids().size());
		assertEquals(1200, result.bounds().width());
		assertEquals(1500, result.bounds().height());
	}

	@Test
	void frameRailsUseExtrusionShapes() {
		OpeningTypeDefinition definition = BuiltinOpeningTypes.fixedWindow();
		var result = new RectangularWindowGenerator().generate(definition, ParameterSet.mergeDefaults(definition.parameters(), ParameterSet.empty()));

		long extrudedRails = result.solids().stream()
			.filter(solid -> solid.componentPath().startsWith("frame."))
			.filter(solid -> solid.shape() instanceof dev.aperture.geometry.shape.ExtrusionShape)
			.count();

		assertEquals(4, extrudedRails);
	}

	@Test
	void goldenMeshBoundsForBottomRail() {
		OpeningTypeDefinition definition = BuiltinOpeningTypes.fixedWindow();
		var result = new RectangularWindowGenerator().generate(definition, ParameterSet.mergeDefaults(definition.parameters(), ParameterSet.empty()));

		GeometrySolid bottomRail = result.solids().stream()
			.filter(solid -> solid.componentPath().equals("frame.bottom"))
			.findFirst()
			.orElseThrow();

		Mesh mesh = ShapeMesher.mesh(bottomRail.shape(), bottomRail.localTransform());

		assertEquals(1200, mesh.bounds().width(), 0.01);
		assertEquals(50, mesh.bounds().height(), 0.01);
		assertEquals(50, mesh.bounds().depth(), 0.01);
		assertEquals(12, mesh.triangleCount());
	}
}
