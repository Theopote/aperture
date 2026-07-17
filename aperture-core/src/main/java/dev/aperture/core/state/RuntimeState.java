package dev.aperture.core.state;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable schema-backed persistent and transient state snapshot. */
public record RuntimeState(
	StateSchema schema,
	Map<String, Object> persistentProperties,
	Map<String, Object> transientProperties,
	long revision,
	Instant timestamp
) {
	public RuntimeState {
		Objects.requireNonNull(schema, "schema");
		persistentProperties = validate(schema, persistentProperties, true);
		transientProperties = validate(schema, transientProperties, false);
		if (revision < 0) throw new IllegalArgumentException("State revision must be non-negative");
		Objects.requireNonNull(timestamp, "timestamp");
	}

	public static RuntimeState initial(StateSchema schema) {
		return new RuntimeState(schema, schema.defaults(true), schema.defaults(false), 0, Instant.EPOCH);
	}

	public static RuntimeState initial(StateSchema schema, Map<String, Object> persistentOverrides) {
		Map<String, Object> persistent = new LinkedHashMap<>(schema.defaults(true));
		persistent.putAll(persistentOverrides);
		return new RuntimeState(schema, persistent, schema.defaults(false), 0, Instant.EPOCH);
	}

	public static RuntimeState restore(
		StateSchema schema,
		Map<String, Object> persistent,
		long revision,
		Instant timestamp
	) {
		Map<String, Object> restored = new LinkedHashMap<>(schema.defaults(true));
		restored.putAll(persistent);
		return new RuntimeState(schema, restored, schema.defaults(false), revision, timestamp);
	}

	public RuntimeState transition(
		Map<String, Object> persistentUpdates,
		Map<String, Object> transientUpdates,
		Instant transitionTime
	) {
		Map<String, Object> persistent = new LinkedHashMap<>(persistentProperties);
		persistent.putAll(persistentUpdates);
		Map<String, Object> transientState = new LinkedHashMap<>(transientProperties);
		transientState.putAll(transientUpdates);
		return new RuntimeState(schema, persistent, transientState, revision + 1, transitionTime);
	}

	public Object value(String name) {
		if (persistentProperties.containsKey(name)) return persistentProperties.get(name);
		if (transientProperties.containsKey(name)) return transientProperties.get(name);
		throw new IllegalArgumentException("Unknown state property: " + name);
	}

	public double number(String name) {
		return ((Number) value(name)).doubleValue();
	}

	public boolean bool(String name) {
		return (Boolean) value(name);
	}

	public String text(String name) {
		return (String) value(name);
	}

	private static Map<String, Object> validate(StateSchema schema, Map<String, Object> values, boolean persistent) {
		Map<String, Object> copy = new LinkedHashMap<>(values);
		copy.forEach((name, value) -> {
			StatePropertyDefinition definition = schema.require(name);
			if (definition.persistent() != persistent) {
				throw new IllegalArgumentException("State property stored in wrong partition: " + name);
			}
			definition.validate(value);
		});
		return Map.copyOf(copy);
	}
}
