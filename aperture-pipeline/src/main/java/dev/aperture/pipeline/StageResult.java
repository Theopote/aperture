package dev.aperture.pipeline;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of executing a {@link PipelineStage}.
 *
 * @param <T> Output type
 */
public sealed interface StageResult<T> {
	/**
	 * Successful execution with output value.
	 */
	record Success<T>(T value) implements StageResult<T> {
		public Success {
			Objects.requireNonNull(value, "value cannot be null");
		}

		@Override
		public boolean isSuccess() {
			return true;
		}

		@Override
		public T getValue() {
			return value;
		}

		@Override
		public Optional<String> getErrorMessage() {
			return Optional.empty();
		}

		@Override
		public Optional<Throwable> getCause() {
			return Optional.empty();
		}
	}

	/**
	 * Failed execution with error message and optional cause.
	 */
	record Failure<T>(String message, Throwable cause) implements StageResult<T> {
		public Failure {
			Objects.requireNonNull(message, "message cannot be null");
		}

		public Failure(String message) {
			this(message, null);
		}

		@Override
		public boolean isSuccess() {
			return false;
		}

		@Override
		public T getValue() {
			throw new IllegalStateException("Cannot get value from failed result: " + message, cause);
		}

		@Override
		public Optional<String> getErrorMessage() {
			return Optional.of(message);
		}

		@Override
		public Optional<Throwable> getCause() {
			return Optional.ofNullable(cause);
		}
	}

	/**
	 * Skipped execution (input unchanged, using cached value).
	 */
	record Skipped<T>(String reason, T cachedValue) implements StageResult<T> {
		public Skipped {
			Objects.requireNonNull(reason, "reason cannot be null");
			Objects.requireNonNull(cachedValue, "cachedValue cannot be null");
		}

		@Override
		public boolean isSuccess() {
			return true;
		}

		@Override
		public T getValue() {
			return cachedValue;
		}

		@Override
		public Optional<String> getErrorMessage() {
			return Optional.empty();
		}

		@Override
		public Optional<Throwable> getCause() {
			return Optional.empty();
		}
	}

	/**
	 * Check if execution was successful.
	 */
	boolean isSuccess();

	/**
	 * Get the output value.
	 *
	 * @return Output value
	 * @throws IllegalStateException if result is a failure
	 */
	T getValue();

	/**
	 * Get error message if execution failed.
	 */
	Optional<String> getErrorMessage();

	/**
	 * Get exception cause if execution failed.
	 */
	Optional<Throwable> getCause();
}
