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
		PipelineMetrics metrics
	) implements PipelineResult {
		public Success {
			Objects.requireNonNull(stageOutputs, "stageOutputs cannot be null");
			Objects.requireNonNull(metrics, "metrics cannot be null");
		}

		@Override
		public boolean isSuccess() {
			return true;
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
}
