package dev.aperture.core.state;

import java.util.Objects;
import java.util.Set;

/** Schema entry for one persistent or transient runtime property. */
public record StatePropertyDefinition(
	StatePropertyType type,
	Object defaultValue,
	Double min,
	Double max,
	Set<String> values,
	boolean persistent
) {
	public StatePropertyDefinition {
		Objects.requireNonNull(type, "type");
		values = Set.copyOf(values);
		validateValue(type, min, max, values, defaultValue);
	}

	public void validate(Object value) {
		Objects.requireNonNull(value, "state value");
		validateValue(type, min, max, values, value);
	}

	private static void validateValue(
		StatePropertyType type, Double min, Double max, Set<String> values, Object value
	) {
		switch (type) {
			case NUMBER -> {
				if (!(value instanceof Number number)) throw invalid(type, value);
				double numeric = number.doubleValue();
				if ((min != null && numeric < min) || (max != null && numeric > max)) throw invalid(type, value);
			}
			case BOOLEAN -> { if (!(value instanceof Boolean)) throw invalid(type, value); }
			case STRING -> { if (!(value instanceof String)) throw invalid(type, value); }
			case ENUM -> {
				if (!(value instanceof String text) || !values.contains(text)) throw invalid(type, value);
			}
		}
	}

	private static IllegalArgumentException invalid(StatePropertyType type, Object value) {
		return new IllegalArgumentException("Invalid " + type + " state value: " + value);
	}

	public static StatePropertyDefinition number(double defaultValue, Double min, Double max, boolean persistent) {
		return new StatePropertyDefinition(StatePropertyType.NUMBER, defaultValue, min, max, Set.of(), persistent);
	}

	public static StatePropertyDefinition bool(boolean defaultValue, boolean persistent) {
		return new StatePropertyDefinition(StatePropertyType.BOOLEAN, defaultValue, null, null, Set.of(), persistent);
	}

	public static StatePropertyDefinition string(String defaultValue, boolean persistent) {
		return new StatePropertyDefinition(StatePropertyType.STRING, defaultValue, null, null, Set.of(), persistent);
	}

	public static StatePropertyDefinition enumeration(String defaultValue, Set<String> values, boolean persistent) {
		return new StatePropertyDefinition(StatePropertyType.ENUM, defaultValue, null, null, values, persistent);
	}
}
