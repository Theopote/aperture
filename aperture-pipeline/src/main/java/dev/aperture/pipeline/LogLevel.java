package dev.aperture.pipeline;

/**
 * Log level for pipeline execution.
 */
public enum LogLevel {
	/**
	 * No logging.
	 */
	NONE,

	/**
	 * Only errors.
	 */
	ERROR,

	/**
	 * Errors and info messages.
	 */
	INFO,

	/**
	 * Errors, info, and debug messages.
	 */
	DEBUG
}
