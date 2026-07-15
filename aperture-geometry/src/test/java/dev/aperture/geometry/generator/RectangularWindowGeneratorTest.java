package dev.aperture.geometry.generator;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RectangularWindowGeneratorTest {
	@Test
	void generatesFrameAndGlazingSolids() {
		OpeningTypeDefinition definition = BuiltinOpeningTypes.fixedWindow();
		ParameterSet parameters = ParameterSet.mergeDefaults(definition.parameters(), ParameterSet.builder()
			.put("mullions", ParameterValue.count(2))
			.build());

		var result = new RectangularWindowGenerator().generate(definition, parameters);

		assertEquals(4, result.solids().size());
		assertEquals(1200, result.bounds().width());
		assertEquals(1500, result.bounds().height());
	}
}
