package dev.aperture.core.validation;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parametric.ParametricValidator;

/**
 * Validates parameter values against the opening type schema (min/max, type match).
 */
public final class ParameterConstraintValidator implements OpeningValidator {
	private final ParametricValidator delegate = new ParametricValidator();

	@Override
	public ValidationResult validate(OpeningTypeDefinition definition, OpeningInstance instance) {
		return delegate.validate(definition, instance);
	}
}
