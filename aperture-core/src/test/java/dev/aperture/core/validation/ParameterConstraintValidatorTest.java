package dev.aperture.core.validation;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParameterConstraintValidatorTest {
	private final ParameterConstraintValidator validator = new ParameterConstraintValidator();

	@Test
	void acceptsDefaultFixedWindowParameters() {
		OpeningTypeDefinition definition = BuiltinOpeningTypes.fixedWindow();
		ParameterSet parameters = definition.resolveParameters(ParameterSet.empty());
		OpeningInstance instance = OpeningInstance.builder(definition.id()).parameters(parameters).build();

		assertTrue(validator.validate(definition, instance).isValid());
	}

	@Test
	void rejectsWidthBelowMinimum() {
		OpeningTypeDefinition definition = BuiltinOpeningTypes.fixedWindow();
		OpeningInstance instance = OpeningInstance.builder(definition.id())
			.parameters(ParameterSet.builder().put("width", ParameterValue.length(100)).build())
			.build();

		assertFalse(validator.validate(definition, instance).isValid());
	}
}
