package dev.aperture.core.placement;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.validation.ValidationResult;

/**
 * Validates a candidate opening against host and neighborhood constraints.
 */
public interface PlacementValidator {
	ValidationResult validate(
		OpeningTypeDefinition definition,
		OpeningInstance candidate,
		PlacementContext context
	);
}
