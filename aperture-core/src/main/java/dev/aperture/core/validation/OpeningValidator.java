package dev.aperture.core.validation;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;

/**
 * Validates opening definitions, instances, or placement contexts.
 */
public interface OpeningValidator {
	ValidationResult validate(OpeningTypeDefinition definition, OpeningInstance instance);
}
