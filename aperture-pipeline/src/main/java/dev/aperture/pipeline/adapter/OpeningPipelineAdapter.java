package dev.aperture.pipeline.adapter;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.geometry.profile.ProfileCatalogLoader;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.opening.pipeline.OpeningGenerationPipeline;
import dev.aperture.pipeline.Pipeline;
import dev.aperture.pipeline.PipelineBuilder;
import dev.aperture.pipeline.PipelineCache;
import dev.aperture.pipeline.PipelineResult;
import dev.aperture.pipeline.stage.CollisionStage;
import dev.aperture.pipeline.stage.ComponentStage;
import dev.aperture.pipeline.stage.ConstraintStage;
import dev.aperture.pipeline.stage.DefinitionStage;
import dev.aperture.pipeline.stage.GeometryStage;
import dev.aperture.pipeline.stage.MeshStage;
import dev.aperture.pipeline.stage.ParameterStage;
import dev.aperture.pipeline.stage.PlacementStage;

/** Creates the configured eight-stage opening generation pipeline. */
public final class OpeningPipelineAdapter {
	private final Pipeline pipeline;

	private OpeningPipelineAdapter(Pipeline pipeline) {
		this.pipeline = pipeline;
	}

	public static OpeningPipelineAdapter standard() {
		return withCache(100, defaultRegistry(), defaultProfiles());
	}

	public static OpeningPipelineAdapter withCache(int cacheCapacity) {
		return withCache(cacheCapacity, defaultRegistry(), defaultProfiles());
	}

	public static OpeningPipelineAdapter withCache(
		int cacheCapacity,
		OpeningTypeRegistry registry,
		ProfileCatalogRegistry profiles
	) {
		Pipeline pipeline = new PipelineBuilder()
			.addStage(new DefinitionStage(registry))
			.addStage(new ParameterStage())
			.addStage(new ConstraintStage())
			.addStage(new ComponentStage())
			.addStage(new GeometryStage(OpeningGenerationPipeline.standard(), profiles))
			.addStage(new MeshStage())
			.addStage(new CollisionStage())
			.addStage(new PlacementStage())
			.withCache(new PipelineCache(cacheCapacity))
			.build();
		return new OpeningPipelineAdapter(pipeline);
	}

	public static OpeningPipelineAdapter withoutCache() {
		return withCache(0, defaultRegistry(), defaultProfiles());
	}

	public static OpeningPipelineAdapter withoutCache(
		OpeningTypeRegistry registry,
		ProfileCatalogRegistry profiles
	) {
		return withCache(0, registry, profiles);
	}

	public PipelineResult execute(String openingTypeId, java.util.Map<String, Object> userParameters) {
		return execute(openingTypeId, userParameters, OpeningState.CLOSED);
	}

	public PipelineResult execute(
		String openingTypeId,
		java.util.Map<String, Object> userParameters,
		OpeningState state
	) {
		return execute(new DefinitionStage.OpeningRequest(openingTypeId, userParameters, state));
	}

	public PipelineResult execute(DefinitionStage.OpeningRequest request) {
		return pipeline.execute(request);
	}

	public Pipeline unwrap() {
		return pipeline;
	}

	public void clearCache() {
		pipeline.clearCache();
	}

	public PipelineCache.CacheStats getCacheStats() {
		return pipeline.cacheStats();
	}

	private static OpeningTypeRegistry defaultRegistry() {
		OpeningTypeRegistry registry = new OpeningTypeRegistry();
		BuiltinOpeningTypes.referenceDefinitions().forEach(registry::register);
		return registry;
	}

	private static ProfileCatalogRegistry defaultProfiles() {
		return new ProfileCatalogLoader().loadClasspathCatalog();
	}
}