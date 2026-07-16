package dev.aperture.pipeline;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Executes a validated sequence of typed generation stages. */
public final class Pipeline {
	private final List<StageRegistration> stages;
	private final ExecutionOptions options;
	private final PipelineCache cache;

	Pipeline(List<StageRegistration> stages, ExecutionOptions options, PipelineCache cache) {
		this.stages = List.copyOf(stages);
		this.options = Objects.requireNonNull(options, "options cannot be null");
		this.cache = Objects.requireNonNull(cache, "cache cannot be null");
	}

	public PipelineResult execute(Object initialInput) {
		Objects.requireNonNull(initialInput, "initialInput cannot be null");
		StageContext ctx = new StageContext(options);
		Map<String, StageOutput> outputs = new LinkedHashMap<>();
		PipelineMetrics.Builder metrics = new PipelineMetrics.Builder();
		long started = System.nanoTime();
		Object currentInput = initialInput;

		ctx.log("Starting pipeline execution with " + stages.size() + " stages");
		for (StageRegistration registration : stages) {
			String stageName = registration.stageName();
			PipelineStage<?, ?> stage = registration.stage();
			if (!stage.inputType().isInstance(currentInput)) {
				return failure(
					stageName,
					"Stage " + stageName + " requires " + stage.inputType().getName()
						+ " but received " + currentInput.getClass().getName(),
					null,
					outputs,
					metrics,
					started
				);
			}

			Optional<StageCacheKey> cacheKey = options.enableCache()
				? cacheKey(stage, currentInput, ctx)
				: Optional.empty();
			if (cacheKey.isPresent()) {
				Object cached = cache.get(cacheKey.get());
				if (cached != null) {
					outputs.put(stageName, new StageOutput(stageName, cached, 0, true));
					metrics.cacheHit().stageSkipped();
					currentInput = cached;
					continue;
				}
				metrics.cacheMiss();
			}

			long stageStarted = System.nanoTime();
			StageResult<?> result = executeStage(stage, currentInput, ctx);
			long duration = System.nanoTime() - stageStarted;
			metrics.stageTime(stageName, duration);
			if (!result.isSuccess()) {
				PipelineDiagnostic diagnostic = result.diagnostic()
					.orElseGet(() -> PipelineDiagnostic.error(DiagnosticCode.INTERNAL_ERROR, stage.id(), "Unknown error", null));
				return failure(diagnostic.withStage(stage.id()), outputs, metrics, started);
			}

			Object output = result.getValue();
			if (output == null || !stage.outputType().isInstance(output)) {
				return failure(
					stageName,
					"Stage " + stageName + " returned an invalid output type",
					null,
					outputs,
					metrics,
					started
				);
			}
			outputs.put(stageName, new StageOutput(stageName, output, duration, false));
			metrics.stageExecuted();
			cacheKey.ifPresent(key -> cache.put(key, output));
			currentInput = output;
		}

		metrics.totalTime(System.nanoTime() - started);
		return new PipelineResult.Success(outputs, metrics.build(), currentInput);
	}

	private static PipelineResult.Failure failure(
		PipelineDiagnostic diagnostic, Map<String, StageOutput> outputs,
		PipelineMetrics.Builder metrics, long started
	) {
		metrics.totalTime(System.nanoTime() - started);
		return new PipelineResult.Failure(diagnostic, outputs);
	}
	private static PipelineResult.Failure failure(
		String stage,
		String message,
		Throwable cause,
		Map<String, StageOutput> outputs,
		PipelineMetrics.Builder metrics,
		long started
	) {
		metrics.totalTime(System.nanoTime() - started);
		return new PipelineResult.Failure(stage, message, cause, outputs);
	}

	@SuppressWarnings("unchecked")
	private static Optional<StageCacheKey> cacheKey(
		PipelineStage<?, ?> stage,
		Object input,
		StageContext context
	) {
		return ((PipelineStage<Object, ?>) stage).cacheKey(input, context);
	}

	@SuppressWarnings("unchecked")
	private static StageResult<?> executeStage(
		PipelineStage<?, ?> stage,
		Object input,
		StageContext context
	) {
		try {
			return ((PipelineStage<Object, ?>) stage).execute(input, context);
		} catch (Exception exception) {
			return new StageResult.Failure<>("Unexpected error in stage " + stage.name(), exception);
		}
	}

	public int stageCount() { return stages.size(); }

	public List<String> stageNames() {
		return stages.stream().map(StageRegistration::stageName).toList();
	}

	public void clearCache() { cache.clear(); }

	public PipelineCache.CacheStats cacheStats() { return cache.getStats(); }

	record StageRegistration(String stageName, PipelineStage<?, ?> stage) {
		StageRegistration {
			Objects.requireNonNull(stageName, "stageName cannot be null");
			Objects.requireNonNull(stage, "stage cannot be null");
		}
	}

	public static PipelineBuilder builder() { return new PipelineBuilder(); }
}