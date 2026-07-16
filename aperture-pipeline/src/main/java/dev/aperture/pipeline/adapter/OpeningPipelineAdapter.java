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
import dev.aperture.pipeline.stage.BoundingBoxCollisionStage;
import dev.aperture.pipeline.stage.ComponentStage;
import dev.aperture.pipeline.stage.ConstraintStage;
import dev.aperture.pipeline.stage.DefinitionStage;
import dev.aperture.pipeline.stage.GeometryStage;
import dev.aperture.pipeline.stage.MeshStage;
import dev.aperture.pipeline.stage.ParameterStage;
import dev.aperture.pipeline.stage.BasicPlacementMetadataStage;

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
		return assemble(
			registry, profiles,
			new dev.aperture.core.constraint.ExpressionConstraintValidator(),
			new ComponentPlanBuilder(), new OpeningGeometryCompiler(), new OpeningMeshCompiler(),
			new BoundingBoxCollisionStage(), new BasicPlacementMetadataStage(),
			new PipelineCache(cacheCapacity)
		);
	}

	public static OpeningPipelineAdapter assemble(
		OpeningTypeRegistry registry,
		ProfileCatalogRegistry profiles,
		dev.aperture.core.constraint.ExpressionConstraintValidator constraintValidator,
		ComponentPlanBuilder componentPlanner,
		OpeningGeometryCompiler geometryCompiler,
		OpeningMeshCompiler meshCompiler,
		BoundingBoxCollisionStage collisionCompiler,
		BasicPlacementMetadataStage placementCompiler,
		PipelineCache cache
	) {
		Pipeline pipeline = new PipelineBuilder()
			.addStage(new DefinitionStage(registry))
			.addStage(new ParameterStage())
			.addStage(new ConstraintStage(constraintValidator))
			.addStage(new ComponentStage(componentPlanner))
			.addStage(new GeometryStage(geometryCompiler, profiles))
			.addStage(new MeshStage(meshCompiler))
			.addStage(collisionCompiler)
			.addStage(placementCompiler)
			.withCache(cache)
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