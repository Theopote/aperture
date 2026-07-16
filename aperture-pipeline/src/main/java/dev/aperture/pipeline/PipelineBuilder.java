package dev.aperture.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builder for constructing {@link Pipeline} instances with fluent API.
 * <p>
 * Example:
 * <pre>{@code
 * Pipeline pipeline = new PipelineBuilder()
 *     .addStage(new ParameterStage())
 *     .addStage(new GeometryStage(geometryCompiler, profiles))
 *     .addStage(new MeshStage(meshCompiler))
 *     .withOptions(ExecutionOptions.DEFAULT.withCache(true))
 *     .build();
 * }</pre>
 */
public final class PipelineBuilder {
	private final List<Pipeline.StageRegistration<?>> stages = new ArrayList<>();
	private ExecutionOptions options = ExecutionOptions.DEFAULT;
	private PipelineCache cache = new PipelineCache(100); // default capacity

	/**
	 * Add a stage to the pipeline.
	 * <p>
	 * Stages are executed in the order they are added.
	 *
	 * @param stage Stage to add
	 * @return This builder
	 */
	public PipelineBuilder addStage(PipelineStage<?, ?> stage) {
		Objects.requireNonNull(stage, "stage cannot be null");
		stages.add(new Pipeline.StageRegistration<>(stage.name(), stage));
		return this;
	}

	/**
	 * Add a stage with explicit name (overrides stage.name()).
	 *
	 * @param name Stage name
	 * @param stage Stage instance
	 * @return This builder
	 */
	public PipelineBuilder addStage(String name, PipelineStage<?, ?> stage) {
		Objects.requireNonNull(name, "name cannot be null");
		Objects.requireNonNull(stage, "stage cannot be null");
		stages.add(new Pipeline.StageRegistration<>(name, stage));
		return this;
	}

	/**
	 * Set execution options.
	 *
	 * @param options Execution options
	 * @return This builder
	 */
	public PipelineBuilder withOptions(ExecutionOptions options) {
		this.options = Objects.requireNonNull(options, "options cannot be null");
		return this;
	}

	/**
	 * Set custom cache instance.
	 *
	 * @param cache Cache instance
	 * @return This builder
	 */
	public PipelineBuilder withCache(PipelineCache cache) {
		this.cache = Objects.requireNonNull(cache, "cache cannot be null");
		return this;
	}

	/**
	 * Set cache capacity.
	 *
	 * @param capacity Maximum number of cached entries
	 * @return This builder
	 */
	public PipelineBuilder withCacheCapacity(int capacity) {
		if (capacity <= 0) {
			throw new IllegalArgumentException("capacity must be positive");
		}
		this.cache = new PipelineCache(capacity);
		return this;
	}

	/**
	 * Disable caching.
	 *
	 * @return This builder
	 */
	public PipelineBuilder withoutCache() {
		this.options = options.withCache(false);
		return this;
	}

	/**
	 * Build the pipeline.
	 *
	 * @return Configured pipeline
	 * @throws IllegalStateException if no stages have been added
	 */
	public Pipeline build() {
		if (stages.isEmpty()) {
			throw new IllegalStateException("Pipeline must have at least one stage");
		}

		// Validate stage chain (optional - could check type compatibility)
		validateStageChain();

		return new Pipeline(stages, options, cache);
	}

	private void validateStageChain() {
		// TODO: Could add runtime type checking here if needed
		// For now, we rely on compile-time type safety where possible
	}

	/**
	 * Get number of stages currently added.
	 */
	public int stageCount() {
		return stages.size();
	}
}
