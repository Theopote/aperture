package dev.aperture.pipeline;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of executing the complete {@link Pipeline}.
 */
public sealed interface PipelineResult {
	/**
	 * Successful pipeline execution with all stage outputs.
	 */
	record Success(
		Map<String, StageOutput> stageOutputs,
		PipelineMetrics metrics,
		Object finalOutput
	) implements PipelineResult {
		public Success(Map<String, StageOutput> stageOutputs, PipelineMetrics metrics) {
			this(stageOutputs, metrics, null);
		}

		public Success {
			Objects.requireNonNull(stageOutputs, "stageOutputs cannot be null");
			Objects.requireNonNull(metrics, "metrics cannot be null");
		}

		@Override
		public boolean isSuccess() {
			return true;
		}

		/**
		 * Get the final output from the last stage, or null if no stages.
		 */
		public Object getFinalOutput() {
			if (finalOutput != null) {
				return finalOutput;
			}
			if (stageOutputs.isEmpty()) {
				return null;
			}
			// Get last stage output
			return stageOutputs.values().stream()
				.reduce((first, second) -> second)
				.map(StageOutput::value)
				.orElse(null);
		}

		/**
		 * Get output from a specific stage.
		 *
		 * @param stageName Stage name
		 * @return Stage output
		 */
		public Optional<StageOutput> getStageOutput(String stageName) {
			return Optional.ofNullable(stageOutputs.get(stageName));
		}

		/**
		 * Get typed value from a specific stage.
		 *
		 * @param stageName Stage name
		 * @return Stage output value
		 */
		@SuppressWarnings("unchecked")
		public <T> Optional<T> getStageValue(String stageName) {
			return getStageOutput(stageName).map(output -> (T) output.value());
		}
	}

	/**
	 * Failed pipeline execution with error details and partial results.
	 */
	record Failure(PipelineDiagnostic diagnostic, Map<String, StageOutput> partialOutputs, PipelineMetrics metrics) implements PipelineResult {
		public Failure { Objects.requireNonNull(diagnostic, "diagnostic"); Objects.requireNonNull(partialOutputs, "partialOutputs"); Objects.requireNonNull(metrics, "metrics"); }
		public Failure(String failedStage, String errorMessage, Throwable cause, Map<String, StageOutput> outputs) {
			this(PipelineDiagnostic.error(DiagnosticCode.INTERNAL_ERROR, StageId.fromExternalName(failedStage), errorMessage, cause), outputs, new PipelineMetrics.Builder().build());
		}
		public Failure(String failedStage, String errorMessage) { this(failedStage, errorMessage, null, Map.of()); }
		public String failedStage() { return diagnostic.stage().externalName(); }
		public String errorMessage() { return diagnostic.message(); }
		public Throwable cause() { return diagnostic.cause(); }
		@Override public boolean isSuccess() { return false; }
		public boolean hasPartialResults() { return !partialOutputs.isEmpty(); }
		public Optional<StageOutput> getPartialOutput(String stageName) { return Optional.ofNullable(partialOutputs.get(stageName)); }
	}
	/**
	 * Check if pipeline execution was successful.
	 */
	boolean isSuccess();

	/**
	 * Get the final output (last stage's output for Success, null for Failure).
	 */
	default Object getFinalOutput() {
		if (this instanceof Success success) {
			return success.getFinalOutput();
		}
		return null;
	}

	/**
	 * Get failure message (null for Success).
	 */
	default String getFailureMessage() {
		if (this instanceof Failure failure) {
			return failure.errorMessage();
		}
		return null;
	}

	/**
	 * Get failure cause (null for Success or if no cause).
	 */
	default Throwable getFailureCause() {
		if (this instanceof Failure failure) {
			return failure.cause();
		}
		return null;
	}

	/**
	 * Get failed stage name (null for Success).
	 */
	default String getFailedStageName() {
		if (this instanceof Failure failure) {
			return failure.failedStage();
		}
		return null;
	}

	/**
	 * Get number of stages executed.
	 */
	default int stageCount() {
		if (this instanceof Success success) {
			return success.stageOutputs().size();
		} else if (this instanceof Failure failure) {
			return failure.partialOutputs().size() + 1;
		}
		return 0;
	}

	/**
	 * Get execution time in milliseconds.
	 */
	default java.time.Duration executionTime() {
		if (this instanceof Success success) {
			return success.metrics().totalExecutionTime();
		} else if (this instanceof Failure failure) {
			return failure.metrics().totalExecutionTime();
		}
		return java.time.Duration.ZERO;
	}

	/**
	 * Get number of cache hits.
	 */
	default int cacheHits() {
		if (this instanceof Success success) {
			return success.metrics().cacheHits();
		}
		return 0;
	}

	/**
	 * Get metrics collected before completion or failure.
	 */
	default PipelineMetrics getMetrics() {
		if (this instanceof Success success) {
			return success.metrics();
		} else if (this instanceof Failure failure) {
			return failure.metrics();
		}
		return null;
	}
}
