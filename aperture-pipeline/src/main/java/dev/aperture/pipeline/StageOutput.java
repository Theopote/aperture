package dev.aperture.pipeline;

import java.util.Objects;

/**
 * Output from a single pipeline stage execution.
 *
 * @param stageName Name of the stage that produced this output
 * @param value Output value (type depends on the stage)
 * @param executionTimeNanos Time taken to execute this stage (nanoseconds)
 * @param fromCache Whether this result came from cache (true) or fresh execution (false)
 */
public record StageOutput(
	String stageName,
	Object value,
	long executionTimeNanos,
	boolean fromCache
) {
	public StageOutput {
		Objects.requireNonNull(stageName, "stageName cannot be null");
		Objects.requireNonNull(value, "value cannot be null");
		if (executionTimeNanos < 0) {
			throw new IllegalArgumentException("executionTimeNanos cannot be negative");
		}
	}

	/**
	 * Get typed value.
	 *
	 * @param <T> Expected type
	 * @return Typed value
	 * @throws ClassCastException if value is not of expected type
	 */
	@SuppressWarnings("unchecked")
	public <T> T typedValue() {
		return (T) value;
	}

	/**
	 * Get typed value with explicit class.
	 *
	 * @param clazz Expected class
	 * @return Typed value
	 * @throws ClassCastException if value is not of expected type
	 */
	public <T> T typedValue(Class<T> clazz) {
		Objects.requireNonNull(clazz, "clazz cannot be null");
		return clazz.cast(value);
	}
}
