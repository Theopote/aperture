package dev.aperture.runtime.model.state;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable, schema-backed runtime snapshot with optimistic revision checks. */
public record RuntimeState(
	StateSchema schema,
	Map<String, StateValue> values,
	StateRevision revision,
	Instant timestamp
) {
	public RuntimeState {
		Objects.requireNonNull(schema, "schema");
		values = validateComplete(schema, values);
		Objects.requireNonNull(revision, "revision");
		Objects.requireNonNull(timestamp, "timestamp");
	}

	public static RuntimeState initial(StateSchema schema) {
		return new RuntimeState(schema, schema.defaults(), StateRevision.INITIAL, Instant.EPOCH);
	}

	public static RuntimeState restore(
		StateSchema schema, Map<String, StateValue> persistentValues, StateRevision revision, Instant timestamp
	) {
		Map<String, StateValue> restored = new LinkedHashMap<>(schema.defaults());
		persistentValues.forEach((name, value) -> {
			StatePropertyDefinition definition = schema.require(name);
			if (definition.persistence() != StatePersistence.PERSISTENT) {
				throw new IllegalArgumentException("Cannot restore non-persistent property: " + name);
			}
			restored.put(name, value);
		});
		return new RuntimeState(schema, restored, revision, timestamp);
	}

	public RuntimeState apply(StatePatch patch) {
		if (!revision.equals(patch.expectedRevision())) {
			throw new StateRevisionConflictException(patch.expectedRevision(), revision);
		}
		Map<String, StateValue> next = new LinkedHashMap<>(values);
		patch.updates().forEach((name, value) -> {
			StatePropertyDefinition definition = schema.require(name);
			if (definition.persistence() == StatePersistence.DERIVED) {
				throw new IllegalArgumentException("Derived property cannot be patched: " + name);
			}
			definition.validate(value);
			next.put(name, value);
		});
		return new RuntimeState(schema, next, revision.next(), patch.timestamp());
	}

	public StateValue value(String name) {
		StateValue value = values.get(name);
		if (value == null) throw new IllegalArgumentException("Unknown state property: " + name);
		return value;
	}

	public Map<String, StateValue> persistentValues() {
		Map<String, StateValue> persistent = new LinkedHashMap<>();
		values.forEach((name, value) -> {
			if (schema.require(name).persistence() == StatePersistence.PERSISTENT) persistent.put(name, value);
		});
		return Map.copyOf(persistent);
	}

	private static Map<String, StateValue> validateComplete(StateSchema schema, Map<String, StateValue> values) {
		Objects.requireNonNull(values, "values");
		if (!values.keySet().equals(schema.properties().keySet())) {
			throw new IllegalArgumentException("Runtime state must contain exactly the schema properties");
		}
		values.forEach((name, value) -> schema.require(name).validate(value));
		return Map.copyOf(values);
	}
}
