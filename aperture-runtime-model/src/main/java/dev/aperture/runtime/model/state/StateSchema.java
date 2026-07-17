package dev.aperture.runtime.model.state;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable declaration of every state property supported by a runtime definition. */
public record StateSchema(String id, int version, Map<String, StatePropertyDefinition> properties) {
	public StateSchema {
		Objects.requireNonNull(id, "id");
		if (id.isBlank()) throw new IllegalArgumentException("State schema ID must not be blank");
		if (version < 1) throw new IllegalArgumentException("State schema version must be >= 1");
		properties = Map.copyOf(properties);
	}

	public StatePropertyDefinition require(String name) {
		StatePropertyDefinition definition = properties.get(name);
		if (definition == null) throw new IllegalArgumentException("Unknown state property: " + name);
		return definition;
	}

	public Map<String, StateValue> defaults() {
		Map<String, StateValue> defaults = new LinkedHashMap<>();
		properties.forEach((name, definition) -> defaults.put(name, definition.defaultValue()));
		return Map.copyOf(defaults);
	}

	public static Builder builder(String id, int version) { return new Builder(id, version); }

	public static final class Builder {
		private final String id;
		private final int version;
		private final Map<String, StatePropertyDefinition> properties = new LinkedHashMap<>();

		private Builder(String id, int version) { this.id = id; this.version = version; }

		public Builder property(String name, StatePropertyDefinition definition) {
			if (name == null || name.isBlank()) throw new IllegalArgumentException("Property name must not be blank");
			if (properties.putIfAbsent(name, Objects.requireNonNull(definition, "definition")) != null) {
				throw new IllegalArgumentException("Duplicate state property: " + name);
			}
			return this;
		}

		public StateSchema build() { return new StateSchema(id, version, properties); }
	}
}
