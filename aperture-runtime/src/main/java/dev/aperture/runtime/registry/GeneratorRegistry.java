package dev.aperture.runtime.registry;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.opening.GeneratorId;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.opening.geometry.generator.OpeningGenerator;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of procedural opening generators.
 */
public final class GeneratorRegistry {
	private final Map<String, OpeningGenerator> generators = new ConcurrentHashMap<>();

	public void register(OpeningGenerator generator) {
		generators.put(generator.id(), generator);
	}

	public Optional<OpeningGenerator> get(GeneratorId id) {
		return Optional.ofNullable(generators.get(id.toString()));
	}

	public OpeningGenerator require(GeneratorId id) {
		return get(id).orElseThrow(() -> new IllegalArgumentException("Unknown generator: " + id));
	}

	public PipelineResult generatePipeline(
		OpeningTypeDefinition definition,
		ParameterSet parameters,
		ProfileCatalogRegistry profiles
	) {
		return generatePipeline(new GenerationContext(definition, parameters, profiles));
	}

	public PipelineResult generatePipeline(GenerationContext context) {
		return require(context.definition().generator()).generate(context);
	}

	public GeometryResult generate(
		OpeningTypeDefinition definition,
		ParameterSet parameters,
		ProfileCatalogRegistry profiles
	) {
		return generatePipeline(definition, parameters, profiles).geometry();
	}
}
