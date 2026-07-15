package dev.aperture.core.placement;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.validation.ValidationResult;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Runs placement validators in sequence and merges their results.
 */
public final class PlacementValidatorChain implements PlacementValidator {
	private final List<PlacementValidator> validators;

	public PlacementValidatorChain(PlacementValidator... validators) {
		this.validators = List.of(validators);
	}

	public PlacementValidatorChain(List<PlacementValidator> validators) {
		this.validators = List.copyOf(validators);
	}

	public static PlacementValidatorChain defaults(OpeningTypeRegistry openingTypes) {
		Objects.requireNonNull(openingTypes, "openingTypes");
		return new PlacementValidatorChain(
			new FitsWithinHostValidator(),
			new NoOverlapValidator(openingTypes)
		);
	}

	@Override
	public ValidationResult validate(
		OpeningTypeDefinition definition,
		OpeningInstance candidate,
		PlacementContext context
	) {
		ValidationResult result = ValidationResult.OK;
		for (PlacementValidator validator : validators) {
			result = result.merge(validator.validate(definition, candidate, context));
		}
		return result;
	}

	@Override
	public String toString() {
		return "PlacementValidatorChain" + Arrays.toString(validators.toArray());
	}
}
