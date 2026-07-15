package dev.aperture.opening.resolve;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.opening.geometry.generator.GenerationTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentPropertyResolverTest {
	@Test
	void panelHingeUsesBoundComponentProperty() {
		var context = GenerationTestSupport.context(BuiltinOpeningTypes.door(), ParameterSet.empty());

		assertEquals("left", ComponentPropertyResolver.panelHinge(context, "right"));
	}

	@Test
	void panelHingePrefersParameterOverride() {
		var context = GenerationTestSupport.context(BuiltinOpeningTypes.door(), ParameterSet.builder()
			.put("hinge_side", ParameterValue.enumValue("right"))
			.build());

		assertEquals("right", ComponentPropertyResolver.panelHinge(context, "left"));
	}
}
