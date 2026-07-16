package dev.aperture.pipeline;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pipeline executes a sequence of {@link PipelineStage}s.
 * <p>
 * Stages are executed in order, with each stage's output becoming
 * the next stage's input. Execution stops on first failure (short-circuit).
 * <p>
 * Features:
 * <ul>
 *   <li>Sequential execution with type safety</li>
 *   <li>Stage-level caching</li>
 *   <li>Short-circuit on failure</li>
 *   <li>Performance metrics</li>
 *   <li>Partial results on failure</li>
 * </ul>
 *
 * @see PipelineBuilder
 */
public final class Pipeline {
	private final List<StageRegistration<?>> stages;
	private final ExecutionOptions options;
	private final PipelineCache cache;

	Pipeline(List<StageRegistration<?>> stages, ExecutionOptions options, PipelineCache cache) {
		this.stages = List.copyOf(stages);
		this.options = Objects.requireNonNull(options, "options cannot be null");
		this.cache = Objects.requireNonNull(cache, "cache cannot be null");

		if (stages.isEmpty()) {
			throw new IllegalArgumentException("Pipeline must have at least one stage");
		}
	}

	/**
	 * Execute the pipeline with given initial input.
	 *
	 * @param initialInput Initial input for first stage
	 * @return Pipeline result (success or failure)
	 */
	public PipelineResult execute(Object initialInput) {
		Objects.requireNonNull(initialInput, "initialInput cannot be null");

		StageContext ctx = new StageContext(options);
		Map<String, StageOutput> outputs = new LinkedHashMap<>();
		PipelineMetrics.Builder metricsBuilder = new PipelineMetrics.Builder();

		long pipelineStartTime = System.currentTimeMillis();
		Object currentInput = initialInput;

		ctx.log("Starting pipeline execution with " + stages.size() + " stages");

		for (StageRegistration<?> registration : stages) {
			String stageName = registration.stageName();

			ctx.debug("Executing stage: " + stageName);

			// Check cache
			if (options.enableCache()) {
				var cached = cache.get(stageName, currentInput);
				if (cached.isPresent()) {
					ctx.debug("Cache hit for stage: " + stageName);
					outputs.put(stageName, new StageOutput(stageName, cached.get(), 0, true));
					metricsBuilder.cacheHit().stageSkipped();
					currentInput = cached.get();
					continue;
				}
			}

			// Execute stage
			long stageStartTime = System.currentTimeMillis();
			StageResult<?> result = executeStage(registration, currentInput, ctx);
			long stageDuration = System.currentTimeMillis() - stageStartTime;

			metricsBuilder.cacheMiss().stageTime(stageName, stageDuration);

			if (!result.isSuccess()) {
				// Stage failed - short circuit
				ctx.error("Stage failed: " + stageName);
				long totalTime = System.currentTimeMillis() - pipelineStartTime;
				metricsBuilder.totalTime(totalTime);

				String errorMessage = result.getErrorMessage().orElse("Unknown error");
				Throwable cause = result.getCause().orElse(null);

				return new PipelineResult.Failure(
					stageName,
					errorMessage,
					cause,
					outputs
				);
			}

			Object stageOutput = result.getValue();
			outputs.put(stageName, new StageOutput(stageName, stageOutput, stageDuration, false));
			metricsBuilder.stageExecuted();

			// Update cache
			if (options.enableCache()) {
				cache.put(stageName, currentInput, stageOutput);
			}

			currentInput = stageOutput;
		}

		long totalTime = System.currentTimeMillis() - pipelineStartTime;
		metricsBuilder.totalTime(totalTime);

		ctx.log("Pipeline execution completed successfully in " + totalTime + "ms");

		return new PipelineResult.Success(outputs, metricsBuilder.build());
	}

	@SuppressWarnings("unchecked")
	private <I, O> StageResult<O> executeStage(
		StageRegistration<?> registration,
		Object input,
		StageContext ctx
	) {
		try {
			PipelineStage<I, O> stage = (PipelineStage<I, O>) registration.stage();
			I typedInput = (I) input;
			return stage.execute(typedInput, ctx);
		} catch (ClassCastException e) {
			return new StageResult.Failure<>(
				"Type mismatch in stage " + registration.stageName(),
				e
			);
		} catch (Exception e) {
			return new StageResult.Failure<>(
				"Unexpected error in stage " + registration.stageName(),
				e
			);
		}
	}

	/**
	 * Get number of stages in this pipeline.
	 */
	public int stageCount() {
		return stages.size();
	}

	/**
	 * Get list of stage names in execution order.
	 */
	public List<String> stageNames() {
		return stages.stream()
			.map(StageRegistration::stageName)
			.toList();
	}

	/**
	 * Clear the pipeline cache.
	 */
	public void clearCache() {
		cache.clear();
	}

	/**
	 * Record for internal stage registration.
	 */
	record StageRegistration<T>(
		String stageName,
		PipelineStage<?, ?> stage
	) {
		StageRegistration {
			Objects.requireNonNull(stageName, "stageName cannot be null");
			Objects.requireNonNull(stage, "stage cannot be null");
		}
	}
}
