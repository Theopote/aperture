package dev.aperture.pipeline;

/**
 * A single stage in the generation pipeline.
 * <p>
 * Each stage transforms input of type {@code I} to output of type {@code O}.
 * Stages are executed sequentially by the {@link Pipeline}.
 *
 * @param <I> Input type
 * @param <O> Output type
 */
public interface PipelineStage<I, O> {
	/**
	 * Execute this stage.
	 *
	 * @param input Input from the previous stage (or initial input for first stage)
	 * @param ctx Execution context for metadata and options
	 * @return Result containing output value or failure
	 */
	StageResult<O> execute(I input, StageContext ctx);

	/**
	 * Name of this stage (used for logging, caching, and metrics).
	 *
	 * @return Stage name (e.g., "parameter", "geometry", "mesh")
	 */
	String name();

	/**
	 * Check if this stage can be skipped because input hasn't changed.
	 * <p>
	 * Default implementation always returns false (never skip).
	 * Stages can override this to enable incremental execution.
	 *
	 * @param input Current input
	 * @param ctx Execution context
	 * @return true if stage can be skipped
	 */
	default boolean canSkip(I input, StageContext ctx) {
		return false;
	}
}
