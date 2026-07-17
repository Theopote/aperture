package dev.aperture.core.state;

import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable schema declaring valid runtime-state properties and their storage class. */
public record StateSchema(Map<String, StatePropertyDefinition> properties) {
	private static final StateSchema EMPTY = new StateSchema(Map.of());

	public StateSchema {
		properties = Map.copyOf(properties);
	}

	public static StateSchema empty() {
		return EMPTY;
	}

	public StatePropertyDefinition require(String name) {
		StatePropertyDefinition definition = properties.get(name);
		if (definition == null) throw new IllegalArgumentException("Unknown state property: " + name);
		return definition;
	}

	public Map<String, Object> defaults(boolean persistent) {
		Map<String, Object> defaults = new LinkedHashMap<>();
		properties.forEach((name, definition) -> {
			if (definition.persistent() == persistent) defaults.put(name, definition.defaultValue());
		});
		return Map.copyOf(defaults);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final Map<String, StatePropertyDefinition> properties = new LinkedHashMap<>();

		public Builder property(String name, StatePropertyDefinition definition) {
			if (name == null || name.isBlank()) throw new IllegalArgumentException("State property name must not be blank");
			if (properties.putIfAbsent(name, definition) != null) throw new IllegalArgumentException("Duplicate state property: " + name);
			return this;
		}

		public StateSchema build() {
			return new StateSchema(properties);
		}
	}
}
