package dev.aperture.kernel;

import dev.aperture.core.instance.OpeningState;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable input to the unified opening generation pipeline. */
public record OpeningRequest(
	String typeId,
	Map<String, Object> parameters,
	OpeningState state,
	OpeningOptions options
) {
	public OpeningRequest(String typeId, Map<String, Object> parameters) {
		this(typeId, parameters, OpeningState.CLOSED, OpeningOptions.DEFAULT);
	}

	public OpeningRequest(String typeId, Map<String, Object> parameters, OpeningOptions options) {
		this(typeId, parameters, OpeningState.CLOSED, options);
	}

	public OpeningRequest {
		Objects.requireNonNull(typeId, "typeId cannot be null");
		Objects.requireNonNull(parameters, "parameters cannot be null");
		Objects.requireNonNull(state, "state cannot be null");
		Objects.requireNonNull(options, "options cannot be null");
		if (typeId.isBlank()) {
			throw new IllegalArgumentException("typeId cannot be blank");
		}
		parameters = Map.copyOf(parameters);
	}

	public void validate() {
		if (!typeId.contains(":")) {
			throw new IllegalArgumentException(
				"typeId must be in format 'namespace:name', got: " + typeId
			);
		}
	}

	public OpeningRequest withParameters(Map<String, Object> newParameters) {
		return new OpeningRequest(typeId, newParameters, state, options);
	}

	public OpeningRequest withState(OpeningState newState) {
		return new OpeningRequest(typeId, parameters, newState, options);
	}

	public OpeningRequest withOptions(OpeningOptions newOptions) {
		return new OpeningRequest(typeId, parameters, state, newOptions);
	}

	public OpeningRequest withParameter(String key, Object value) {
		Map<String, Object> newParameters = new HashMap<>(parameters);
		newParameters.put(key, value);
		return new OpeningRequest(typeId, newParameters, state, options);
	}
}