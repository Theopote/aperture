package dev.aperture.core.placement;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.geometry.primitives.BoundingBox;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.validation.ValidationIssue;
import dev.aperture.core.validation.ValidationResult;

/**
 * Ensures the opening footprint fits inside the host's available bounds.
 */
public final class FitsWithinHostValidator implements PlacementValidator {
	@Override
	public ValidationResult validate(
		OpeningTypeDefinition definition,
		OpeningInstance candidate,
		PlacementContext context
	) {
		return OpeningFootprint.worldBounds(definition, candidate)
			.map(footprint -> validateFootprint(footprint, context.hostBounds()))
			.orElse(ValidationResult.OK);
	}

	private static ValidationResult validateFootprint(BoundingBox footprint, BoundingBox hostBounds) {
		if (contains(hostBounds, footprint)) {
			return ValidationResult.OK;
		}
		return ValidationResult.of(ValidationIssue.error(
			"placement.fits_within_host",
			"Opening footprint exceeds host bounds"
		));
	}

	private static boolean contains(BoundingBox outer, BoundingBox inner) {
		return inner.min().x() >= outer.min().x()
			&& inner.min().y() >= outer.min().y()
			&& inner.min().z() >= outer.min().z()
			&& inner.max().x() <= outer.max().x()
			&& inner.max().y() <= outer.max().y()
			&& inner.max().z() <= outer.max().z();
	}
}
