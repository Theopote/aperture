package dev.aperture.pipeline;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Execution context passed to each {@link PipelineStage}.
 * <p>
 * Provides:
 * <ul>
 *   <li>Metadata storage for stage-to-stage communication</li>
 *   <li>Execution options (caching, timeouts, etc.)</li>
 *   <li>Logging capabilities</li>
 * </ul>
 */
public final class StageContext {
	private final Map<String, Object> metadata;
	private final ExecutionOptions options;

	public StageContext(ExecutionOptions options) {
		this.metadata = new HashMap<>();
		this.options = Objects.requireNonNull(options, "options cannot be null");
	}

	/**
	 * Get metadata value by key.
	 *
	 * @param key Metadata key
	 * @return Optional containing value if present
	 */
	@SuppressWarnings("unchecked")
	public <T> Optional<T> getMetadata(String key) {
		Objects.requireNonNull(key, "key cannot be null");
		return Optional.ofNullable((T) metadata.get(key));
	}

	/**
	 * Store metadata value.
	 *
	 * @param key Metadata key
	 * @param value Metadata value
	 */
	public void putMetadata(String key, Object value) {
		Objects.requireNonNull(key, "key cannot be null");
		Objects.requireNonNull(value, "value cannot be null");
		metadata.put(key, value);
	}

	/**
	 * Check if metadata key exists.
	 */
	public boolean hasMetadata(String key) {
		Objects.requireNonNull(key, "key cannot be null");
		return metadata.containsKey(key);
	}

	/**
	 * Get execution options.
	 */
	public ExecutionOptions options() {
		return options;
	}

	/**
	 * Log a message (delegates to options logger if configured).
	 */
	public void log(String message) {
		Objects.requireNonNull(message, "message cannot be null");
		// TODO: Implement logging based on options
		if (options.logLevel().ordinal() >= LogLevel.INFO.ordinal()) {
			System.out.println("[Pipeline] " + message);
		}
	}

	/**
	 * Log a debug message.
	 */
	public void debug(String message) {
		Objects.requireNonNull(message, "message cannot be null");
		if (options.logLevel().ordinal() >= LogLevel.DEBUG.ordinal()) {
			System.out.println("[Pipeline DEBUG] " + message);
		}
	}

	/**
	 * Log an error message.
	 */
	public void error(String message) {
		Objects.requireNonNull(message, "message cannot be null");
		if (options.logLevel().ordinal() >= LogLevel.ERROR.ordinal()) {
			System.err.println("[Pipeline ERROR] " + message);
		}
	}
}
