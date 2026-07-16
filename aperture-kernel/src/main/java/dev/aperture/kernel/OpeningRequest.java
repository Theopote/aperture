package dev.aperture.kernel;

import java.util.Map;
import java.util.Objects;

/**
 * Request for opening generation.
 * <p>
 * Contains the opening type identifier, user parameters, and generation options.
 * Immutable and thread-safe.
 */
public record OpeningRequest(
	String typeId,
	Map<String, Object> parameters,
	OpeningOptions options
) {
	/**
	 * Create request with default options.
	 */
	public OpeningRequest(String typeId, Map<String, Object> parameters) {
		this(typeId, parameters, OpeningOptions.DEFAULT);
	}

	/**
	 * Compact constructor with validation.
	 */
	public OpeningRequest {
		Objects.requireNonNull(typeId, "typeId cannot be null");
		Objects.requireNonNull(parameters, "parameters cannot be null");
		Objects.requireNonNull(options, "options cannot be null");

		if (typeId.isBlank()) {
			throw new IllegalArgumentException("typeId cannot be blank");
		}

		// Defensive copy for immutability
		parameters = Map.copyOf(parameters);
	}

	/**
	 * Validate this request.
	 * <p>
	 * Checks that the type ID is valid format and parameters are non-null.
	 * Does not validate parameter values against the schema.
	 *
	 * @throws IllegalArgumentException if validation fails
	 */
	public void validate() {
		// Basic validation already done in constructor
		// Additional validation could include:
		// - Type ID format (e.g., must contain ':')
		// - Parameter value types

		if (!typeId.contains(":")) {
			throw new IllegalArgumentException(
				"typeId must be in format 'namespace:name', got: " + typeId
			);
		}
	}

	/**
	 * Create a new request with modified parameters.
	 */
	public OpeningRequest withParameters(Map<String, Object> newParameters) {
		return new OpeningRequest(typeId, newParameters, options);
	}

	/**
	 * Create a new request with modified options.
	 */
	public OpeningRequest withOptions(OpeningOptions newOptions) {
		return new OpeningRequest(typeId, parameters, newOptions);
	}

	/**
	 * Create a new request with an additional parameter.
	 */
	public OpeningRequest withParameter(String key, Object value) {
		var newParams = new java.util.HashMap<>(parameters);
		newParams.put(key, value);
		return new OpeningRequest(typeId, newParams, options);
	}
}
