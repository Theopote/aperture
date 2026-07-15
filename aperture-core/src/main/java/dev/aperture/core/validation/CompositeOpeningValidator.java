package dev.aperture.core.validation;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;

import java.util.Arrays;

/**
 * Runs multiple opening validators in sequence.
 */
public record CompositeOpeningValidator(OpeningValidator... validators) implements OpeningValidator {
	public CompositeOpeningValidator {
		validators = Arrays.copyOf(validators, validators.length);
	}

	public static OpeningValidator schemaAndConstraints(OpeningValidator schemaValidator) {
		return new CompositeOpeningValidator(
			schemaValidator,
			new dev.aperture.core.constraint.ExpressionConstraintValidator()
		);
	}

	@Override
	public ValidationResult validate(OpeningTypeDefinition definition, OpeningInstance instance) {
		ValidationResult result = ValidationResult.OK;
		for (OpeningValidator validator : validators) {
			result = result.merge(validator.validate(definition, instance));
		}
		return result;
	}
}
