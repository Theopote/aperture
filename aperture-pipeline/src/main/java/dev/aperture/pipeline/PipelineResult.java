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
	record Failure(
		String failedStage,
		String errorMessage,
		Throwable cause,
		Map<String, StageOutput> partialOutputs
	) implements PipelineResult {
		public Failure {
			Objects.requireNonNull(failedStage, "failedStage cannot be null");
			Objects.requireNonNull(errorMessage, "errorMessage cannot be null");
			Objects.requireNonNull(partialOutputs, "partialOutputs cannot be null");
		}

		public Failure(String failedStage, String errorMessage) {
			this(failedStage, errorMessage, null, Map.of());
		}

		@Override
		public boolean isSuccess() {
			return false;
		}

		/**
		 * Check if there are any partial outputs from stages before failure.
		 */
		public boolean hasPartialResults() {
			return !partialOutputs.isEmpty();
		}

		/**
		 * Get partial output from a specific stage (executed before failure).
		 */
		public Optional<StageOutput> getPartialOutput(String stageName) {
			return Optional.ofNullable(partialOutputs.get(stageName));
		}
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
			return failure.partialOutputs().size();
		}
		return 0;
	}

	/**
	 * Get execution time in milliseconds.
	 */
	default long executionTimeMs() {
		if (this instanceof Success success) {
			return success.metrics().totalExecutionTimeMs();
		}
		return 0;
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
	 * Get metrics (null for Failure).
	 */
	default PipelineMetrics getMetrics() {
		if (this instanceof Success success) {
			return success.metrics();
		}
		return null;
	}
}
