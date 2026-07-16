package dev.aperture.kernel;

import java.util.Objects;

/**
 * Result of partial pipeline execution (up to a specific stage).
 * <p>
 * Sealed interface with Success and Failure variants.
 *
 * @param <T> Output type of the target stage
 */
public sealed interface PartialResult<T> {

	/**
	 * Successful partial execution.
	 */
	record Success<T>(
		String typeId,
		String stageName,
		T value,
		int stagesExecuted,
		GenerationMetrics metrics
	) implements PartialResult<T> {
		public Success {
			Objects.requireNonNull(typeId, "typeId cannot be null");
			Objects.requireNonNull(stageName, "stageName cannot be null");
			Objects.requireNonNull(value, "value cannot be null");
			Objects.requireNonNull(metrics, "metrics cannot be null");
		}

		@Override
		public boolean isSuccess() {
			return true;
		}

		@Override
		public String typeId() {
			return typeId;
		}

		/**
		 * Get the stage output value.
		 */
		public T getValue() {
			return value;
		}
	}

	/**
	 * Failed partial execution.
	 */
	record Failure<T>(
		String typeId,
		String failedStage,
		String errorMessage,
		Throwable cause
	) implements PartialResult<T> {
		public Failure {
			Objects.requireNonNull(typeId, "typeId cannot be null");
			Objects.requireNonNull(failedStage, "failedStage cannot be null");
			Objects.requireNonNull(errorMessage, "errorMessage cannot be null");
		}

		public Failure(String typeId, String failedStage, String errorMessage) {
			this(typeId, failedStage, errorMessage, null);
		}

		@Override
		public boolean isSuccess() {
			return false;
		}

		@Override
		public String typeId() {
			return typeId;
		}
	}

	/**
	 * Check if execution was successful.
	 */
	boolean isSuccess();

	/**
	 * Get the opening type ID.
	 */
	String typeId();

	/**
	 * Get as Success or throw.
	 */
	default Success<T> asSuccess() {
		if (this instanceof Success<T> s) {
			return s;
		}
		throw new IllegalStateException("Result is not successful");
	}

	/**
	 * Get as Failure or throw.
	 */
	default Failure<T> asFailure() {
		if (this instanceof Failure<T> f) {
			return f;
		}
		throw new IllegalStateException("Result is not a failure");
	}
}
