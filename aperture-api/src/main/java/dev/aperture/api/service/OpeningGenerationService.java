package dev.aperture.api.service;

import dev.aperture.api.registry.GeneratorRegistry;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.validation.CompositeOpeningValidator;
import dev.aperture.core.validation.OpeningValidator;
import dev.aperture.core.validation.ParameterConstraintValidator;
import dev.aperture.core.validation.ValidationResult;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;

/**
 * Orchestrates validation and procedural generation for a placed opening instance.
 */
public final class OpeningGenerationService {
	private final OpeningTypeRegistry openingTypes;
	private final GeneratorRegistry generators;
	private final ProfileCatalogRegistry profiles;
	private final OpeningValidator parameterValidator;

	public OpeningGenerationService(
		OpeningTypeRegistry openingTypes,
		GeneratorRegistry generators,
		ProfileCatalogRegistry profiles
	) {
		this(openingTypes, generators, profiles, CompositeOpeningValidator.schemaAndConstraints(new ParameterConstraintValidator()));
	}

	public OpeningGenerationService(
		OpeningTypeRegistry openingTypes,
		GeneratorRegistry generators,
		ProfileCatalogRegistry profiles,
		OpeningValidator parameterValidator
	) {
		this.openingTypes = openingTypes;
		this.generators = generators;
		this.profiles = profiles;
		this.parameterValidator = parameterValidator;
	}

	public GeometryResult generate(OpeningInstance instance) {
		return generatePipeline(instance).geometry();
	}

	public PipelineResult generatePipeline(OpeningInstance instance) {
		OpeningTypeDefinition definition = openingTypes.require(instance.typeId());
		ParameterSet resolved = ParameterSet.mergeDefaults(definition.parameters(), instance.parameters());

		ValidationResult validation = parameterValidator.validate(
			definition,
			instance.withParameters(resolved)
		);
		if (!validation.isValid()) {
			throw new IllegalStateException("Opening instance failed validation: " + validation.issues());
		}

		return generators.generatePipeline(definition, resolved, profiles);
	}
}
