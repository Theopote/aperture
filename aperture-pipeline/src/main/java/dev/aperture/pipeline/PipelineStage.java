package dev.aperture.pipeline;

import java.util.Optional;

/** A typed transformation in the generation pipeline. */
public interface PipelineStage<I, O> {
	StageResult<O> execute(I input, StageContext ctx);

	String name();

	default StageId id() {
		return StageId.fromExternalName(name());
	}

	/** Runtime input token used to validate the stage chain before execution. */
	default Class<?> inputType() {
		return Object.class;
	}

	/** Runtime output token used to validate the stage chain before execution. */
	default Class<?> outputType() {
		return Object.class;
	}

	/** Explicit cache key; an empty result disables caching for this stage. */
	default Optional<StageCacheKey> cacheKey(I input, StageContext ctx) {
		return Optional.empty();
	}

	default boolean canSkip(I input, StageContext ctx) {
		return false;
	}
}