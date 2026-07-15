package dev.aperture.core.parametric;

import dev.aperture.core.parameter.ParameterDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Schema of all parameters on an opening type.
 */
public final class ParametricSchema {
	private final Map<String, Parameter> parameters;

	public ParametricSchema(Map<String, Parameter> parameters) {
		this.parameters = Map.copyOf(parameters);
	}

	public static ParametricSchema empty() {
		return new ParametricSchema(Map.of());
	}

	public static ParametricSchema of(Map<String, Parameter> parameters) {
		return new ParametricSchema(parameters);
	}

	public static ParametricSchema fromLegacy(Map<String, ParameterDefinition> legacy) {
		return new ParametricSchema(ParameterBridge.fromLegacyMap(legacy));
	}

	public Map<String, Parameter> parameters() {
		return parameters;
	}

	public List<String> names() {
		return List.copyOf(parameters.keySet());
	}

	public Optional<Parameter> get(String name) {
		return Optional.ofNullable(parameters.get(name));
	}

	public Parameter require(String name) {
		return get(name).orElseThrow(() -> new IllegalArgumentException("Unknown parameter: " + name));
	}

	public Map<String, ParameterDefinition> toLegacyMap() {
		return ParameterBridge.toLegacyMap(parameters);
	}

	public ParameterSet mergeDefaults(ParameterSet overrides) {
		Objects.requireNonNull(overrides, "overrides");
		return ParameterSet.mergeDefaults(toLegacyMap(), overrides);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final Map<String, Parameter> parameters = new LinkedHashMap<>();

		public Builder put(String name, Parameter parameter) {
			parameters.put(name, parameter);
			return this;
		}

		public Builder putAll(Map<String, Parameter> other) {
			parameters.putAll(other);
			return this;
		}

		public ParametricSchema build() {
			return new ParametricSchema(parameters);
		}
	}
}
