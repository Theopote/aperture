package dev.aperture.core.placement;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.geometry.primitives.Transform3d;
import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningInstanceStore;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.validation.OpeningValidator;
import dev.aperture.core.validation.ParameterConstraintValidator;
import dev.aperture.core.validation.ValidationResult;

/**
 * Orchestrates preview creation, validation, and commit for opening placement.
 */
public final class PlacementService {
	private final OpeningTypeRegistry openingTypes;
	private final OpeningInstanceStore instances;
	private final OpeningValidator parameterValidator;
	private final PlacementValidator placementValidator;

	public PlacementService(OpeningTypeRegistry openingTypes, OpeningInstanceStore instances) {
		this(
			openingTypes,
			instances,
			new ParameterConstraintValidator(),
			PlacementValidatorChain.defaults(openingTypes)
		);
	}

	public PlacementService(
		OpeningTypeRegistry openingTypes,
		OpeningInstanceStore instances,
		OpeningValidator parameterValidator,
		PlacementValidator placementValidator
	) {
		this.openingTypes = openingTypes;
		this.instances = instances;
		this.parameterValidator = parameterValidator;
		this.placementValidator = placementValidator;
	}

	public PlacementSession preview(
		OpeningId typeId,
		ParameterSet parameterOverrides,
		Transform3d transform,
		HostBinding host,
		PlacementContext context
	) {
		OpeningTypeDefinition definition = openingTypes.require(typeId);
		ParameterSet parameters = ParameterSet.mergeDefaults(definition.parameters(), parameterOverrides);
		OpeningInstance candidate = OpeningInstance.builder(typeId)
			.parameters(parameters)
			.transform(transform)
			.host(host)
			.build();

		ValidationResult validation = parameterValidator.validate(definition, candidate)
			.merge(placementValidator.validate(definition, candidate, context));

		return new PlacementSession(typeId, parameterOverrides, candidate, host, validation);
	}

	public OpeningInstance commit(PlacementSession session) {
		if (!session.isValid()) {
			throw new IllegalStateException("Cannot commit invalid placement: " + session.validationReport().issues());
		}
		instances.put(session.previewInstance());
		return session.previewInstance();
	}
}
