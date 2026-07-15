package dev.aperture.runtime.service;

import dev.aperture.runtime.registry.GeneratorRegistry;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.validation.CompositeOpeningValidator;
import dev.aperture.core.validation.OpeningValidator;
import dev.aperture.core.validation.ParameterConstraintValidator;
import dev.aperture.core.validation.ValidationResult;
import dev.aperture.geometry.export.GeometryExport;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.geometry.recipe.GeometryRecipe;
import dev.aperture.geometry.recipe.io.GeometryRecipeCodec;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.geometry.pipeline.CompiledPipeline;
import dev.aperture.opening.pipeline.OpeningGenerationPipeline;

/**
 * Orchestrates validation and procedural generation for a placed opening instance.
 */
public final class OpeningGenerationService {
	private final OpeningTypeRegistry openingTypes;
	private final GeneratorRegistry generators;
	private final ProfileCatalogRegistry profiles;
	private final OpeningValidator parameterValidator;
	private final OpeningGenerationPipeline pipeline;

	public OpeningGenerationService(
		OpeningTypeRegistry openingTypes,
		GeneratorRegistry generators,
		ProfileCatalogRegistry profiles
	) {
		this(
			openingTypes,
			generators,
			profiles,
			CompositeOpeningValidator.schemaAndConstraints(new ParameterConstraintValidator()),
			OpeningGenerationPipeline.standard()
		);
	}

	public OpeningGenerationService(
		OpeningTypeRegistry openingTypes,
		GeneratorRegistry generators,
		ProfileCatalogRegistry profiles,
		OpeningValidator parameterValidator
	) {
		this(openingTypes, generators, profiles, parameterValidator, OpeningGenerationPipeline.standard());
	}

	public OpeningGenerationService(
		OpeningTypeRegistry openingTypes,
		GeneratorRegistry generators,
		ProfileCatalogRegistry profiles,
		OpeningValidator parameterValidator,
		OpeningGenerationPipeline pipeline
	) {
		this.openingTypes = openingTypes;
		this.generators = generators;
		this.profiles = profiles;
		this.parameterValidator = parameterValidator;
		this.pipeline = pipeline;
	}

	public GeometryResult generate(OpeningInstance instance) {
		return generatePipeline(instance).geometry();
	}

	public PipelineResult generatePipeline(OpeningInstance instance) {
		GenerationContext context = validatedContext(instance);
		return generators.generatePipeline(context.definition(), context.parameters(), profiles);
	}

	public CompiledPipeline compile(OpeningInstance instance) {
		return pipeline.compile(validatedContext(instance));
	}

	public GeometryRecipe compileRecipe(OpeningInstance instance) {
		return compile(instance).recipe();
	}

	public String exportRecipeJson(OpeningInstance instance) {
		return GeometryRecipeCodec.toJson(compileRecipe(instance));
	}

	public String exportGltf(OpeningInstance instance) {
		return GeometryExport.toGltf(compileRecipe(instance));
	}

	private GenerationContext validatedContext(OpeningInstance instance) {
		OpeningTypeDefinition definition = openingTypes.require(instance.typeId());
		ParameterSet resolved = definition.resolveParameters(instance.parameters());

		ValidationResult validation = parameterValidator.validate(definition, instance);
		if (!validation.isValid()) {
			throw new IllegalStateException("Opening instance failed validation: " + validation.issues());
		}

		return new GenerationContext(definition, resolved, profiles);
	}
}
