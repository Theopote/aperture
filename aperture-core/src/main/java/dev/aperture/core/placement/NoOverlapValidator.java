package dev.aperture.core.placement;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.geometry.primitives.BoundingBox;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.validation.ValidationIssue;
import dev.aperture.core.validation.ValidationResult;

import java.util.Objects;

/**
 * Rejects placements whose footprint intersects another opening on the same host anchor.
 */
public final class NoOverlapValidator implements PlacementValidator {
	private final OpeningTypeRegistry openingTypes;

	public NoOverlapValidator(OpeningTypeRegistry openingTypes) {
		this.openingTypes = Objects.requireNonNull(openingTypes, "openingTypes");
	}

	@Override
	public ValidationResult validate(
		OpeningTypeDefinition definition,
		OpeningInstance candidate,
		PlacementContext context
	) {
		return OpeningFootprint.worldBounds(definition, candidate)
			.map(footprint -> validateNoOverlap(footprint, candidate, context))
			.orElse(ValidationResult.OK);
	}

	private ValidationResult validateNoOverlap(
		BoundingBox footprint,
		OpeningInstance candidate,
		PlacementContext context
	) {
		for (OpeningInstance existing : context.existingInstances()) {
			if (existing.instanceId().equals(candidate.instanceId())) {
				continue;
			}
			if (!sameHost(existing, candidate)) {
				continue;
			}

			OpeningTypeDefinition existingDefinition = openingTypes.require(existing.typeId());
			var existingBounds = OpeningFootprint.worldBounds(existingDefinition, existing);
			if (existingBounds.isPresent() && intersects(footprint, existingBounds.get())) {
				return ValidationResult.of(ValidationIssue.error(
					"placement.no_overlap",
					"Opening overlaps existing instance " + existing.instanceId()
				));
			}
		}
		return ValidationResult.OK;
	}

	private static boolean sameHost(OpeningInstance left, OpeningInstance right) {
		return left.host().type() == right.host().type()
			&& left.host().anchor().equals(right.host().anchor());
	}

	private static boolean intersects(BoundingBox left, BoundingBox right) {
		return left.min().x() < right.max().x()
			&& left.max().x() > right.min().x()
			&& left.min().y() < right.max().y()
			&& left.max().y() > right.min().y()
			&& left.min().z() < right.max().z()
			&& left.max().z() > right.min().z();
	}
}
