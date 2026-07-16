package dev.aperture.pipeline.adapter;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.opening.compile.OpeningGeometryCompiler;
import dev.aperture.opening.compile.OpeningMeshCompiler;
import dev.aperture.opening.component.ComponentPlanBuilder;
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

/** Configures the sole production orchestration pipeline for opening generation. */
public final class OpeningPipelineAdapter {
	private final Pipeline pipeline;

	private OpeningPipelineAdapter(Pipeline pipeline) {
		this.pipeline = pipeline;
	}

	public static OpeningPipelineAdapter standard(
		OpeningTypeRegistry registry,
		ProfileCatalogRegistry profiles
	) {
		return withCache(100, registry, profiles);
	}

	public static OpeningPipelineAdapter withCache(
		int cacheCapacity,
		OpeningTypeRegistry registry,
		ProfileCatalogRegistry profiles
	) {
		return withCache(
			cacheCapacity,
			registry,
			profiles,
			new ComponentPlanBuilder(),
			new OpeningGeometryCompiler(),
			new OpeningMeshCompiler()
		);
	}

	public static OpeningPipelineAdapter withCache(
		int cacheCapacity,
		OpeningTypeRegistry registry,
		ProfileCatalogRegistry profiles,
		ComponentPlanBuilder componentPlanner,
		OpeningGeometryCompiler geometryCompiler,
		OpeningMeshCompiler meshCompiler
	) {
		Pipeline pipeline = new PipelineBuilder()
			.addStage(new DefinitionStage(registry))
			.addStage(new ParameterStage())
			.addStage(new ConstraintStage())
			.addStage(new ComponentStage(componentPlanner))
			.addStage(new GeometryStage(geometryCompiler, profiles))
			.addStage(new MeshStage(meshCompiler))
			.addStage(new CollisionStage())
			.addStage(new PlacementStage())
			.withCache(new PipelineCache(cacheCapacity))
			.build();
		return new OpeningPipelineAdapter(pipeline);
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
}