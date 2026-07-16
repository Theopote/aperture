package dev.aperture.pipeline.adapter;

import dev.aperture.pipeline.Pipeline;
import dev.aperture.pipeline.PipelineCache;
import dev.aperture.pipeline.PipelineResult;
import dev.aperture.pipeline.stage.*;

/**
 * Adapter that creates a configured Pipeline for opening generation.
 * <p>
 * This bridges the unified pipeline system with the existing opening
 * generation infrastructure, providing a drop-in replacement for
 * {@code OpeningGenerationPipeline}.
 * <p>
 * The adapter configures all 8 standard stages and provides factory
 * methods for different use cases (with/without cache, custom config).
 */
public final class OpeningPipelineAdapter {

	private final Pipeline pipeline;

	private OpeningPipelineAdapter(Pipeline pipeline) {
		this.pipeline = pipeline;
	}

	/**
	 * Create standard opening generation pipeline with default configuration.
	 * <p>
	 * Includes:
	 * <ul>
	 *   <li>All 8 standard stages</li>
	 *   <li>LRU cache (capacity: 100)</li>
	 *   <li>Debug logging disabled</li>
	 * </ul>
	 */
	public static OpeningPipelineAdapter standard() {
		return withCache(100);
	}

	/**
	 * Create pipeline with custom cache capacity.
	 *
	 * @param cacheCapacity Maximum number of cached stage results (0 to disable)
	 */
	public static OpeningPipelineAdapter withCache(int cacheCapacity) {
		PipelineCache cache = new PipelineCache(cacheCapacity);

		Pipeline pipeline = Pipeline.builder()
			.addStage(new DefinitionStage())
			.addStage(new ParameterStage())
			.addStage(new ConstraintStage())
			.addStage(new ComponentStage())
			.addStage(new GeometryStage())
			.addStage(new MeshStage())
			.addStage(new CollisionStage())
			.addStage(new PlacementStage())
			.withCache(cache)
			.build();

		return new OpeningPipelineAdapter(pipeline);
	}

	/**
	 * Create pipeline without caching.
	 * <p>
	 * Useful for testing or when caching is not beneficial (e.g., all unique inputs).
	 */
	public static OpeningPipelineAdapter withoutCache() {
		return withCache(0);
	}

	/**
	 * Execute complete pipeline from opening type ID and parameters.
	 *
	 * @param openingTypeId Opening type identifier (e.g., "aperture:door_standard")
	 * @param userParameters User-provided parameter overrides
	 * @return Pipeline execution result
	 */
	public PipelineResult execute(String openingTypeId, java.util.Map<String, Object> userParameters) {
		var request = new DefinitionStage.OpeningRequest(openingTypeId, userParameters);
		return pipeline.execute(request);
	}

	/**
	 * Execute complete pipeline from prepared request.
	 *
	 * @param request Opening generation request
	 * @return Pipeline execution result
	 */
	public PipelineResult execute(DefinitionStage.OpeningRequest request) {
		return pipeline.execute(request);
	}

	/**
	 * Get the underlying pipeline for advanced usage.
	 * <p>
	 * Use this if you need direct access to pipeline methods like
	 * {@code clearCache()} or {@code stageNames()}.
	 */
	public Pipeline unwrap() {
		return pipeline;
	}

	/**
	 * Clear the pipeline cache.
	 * <p>
	 * Call this when opening definitions or registry contents have changed
	 * to ensure stale cached results are not used.
	 */
	public void clearCache() {
		pipeline.clearCache();
	}

	/**
	 * Get cache statistics for monitoring.
	 */
	public PipelineCache.CacheStats getCacheStats() {
		// Access cache through reflection or add getter to Pipeline
		// For now, return placeholder
		return new PipelineCache.CacheStats(0, 100, 0, 0);
	}
}
