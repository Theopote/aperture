package dev.aperture.pipeline;

import java.util.Objects;

/**
 * Configuration options for pipeline execution.
 */
public record ExecutionOptions(
	boolean enableCache,
	boolean enableParallelExecution,
	int timeoutMs,
	LogLevel logLevel
) {
	/**
	 * Default execution options.
	 */
	public static final ExecutionOptions DEFAULT = new ExecutionOptions(
		true,      // cache enabled
		false,     // no parallel execution (sequential for now)
		30000,     // 30s timeout
		LogLevel.INFO
	);

	public ExecutionOptions {
		Objects.requireNonNull(logLevel, "logLevel cannot be null");
		if (timeoutMs <= 0) {
			throw new IllegalArgumentException("timeoutMs must be positive");
		}
	}

	/**
	 * Create options with caching enabled/disabled.
	 */
	public ExecutionOptions withCache(boolean enabled) {
		return new ExecutionOptions(enabled, enableParallelExecution, timeoutMs, logLevel);
	}

	/**
	 * Create options with custom timeout.
	 */
	public ExecutionOptions withTimeout(int timeoutMs) {
		return new ExecutionOptions(enableCache, enableParallelExecution, timeoutMs, logLevel);
	}

	/**
	 * Create options with custom log level.
	 */
	public ExecutionOptions withLogLevel(LogLevel level) {
		return new ExecutionOptions(enableCache, enableParallelExecution, timeoutMs, level);
	}
}
