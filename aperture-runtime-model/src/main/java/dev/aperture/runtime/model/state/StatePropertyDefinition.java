package dev.aperture.runtime.model.state;

import java.util.Objects;
import java.util.Set;

/** Schema contract for one typed runtime property. */
public record StatePropertyDefinition(
	StateValueType type,
	StateValue defaultValue,
	StatePersistence persistence,
	StateDistribution distribution,
	Double minimum,
	Double maximum,
	Set<String> enumValues
) {
	public StatePropertyDefinition {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(defaultValue, "defaultValue");
		Objects.requireNonNull(persistence, "persistence");
		Objects.requireNonNull(distribution, "distribution");
		enumValues = Set.copyOf(enumValues);
		if (minimum != null && maximum != null && minimum > maximum) {
			throw new IllegalArgumentException("minimum must not exceed maximum");
		}
		if (type != StateValueType.NUMBER && (minimum != null || maximum != null)) {
			throw new IllegalArgumentException("Only number state can declare a numeric range");
		}
		if (type == StateValueType.ENUM && enumValues.isEmpty()) {
			throw new IllegalArgumentException("Enum state requires allowed values");
		}
		if (type != StateValueType.ENUM && !enumValues.isEmpty()) {
			throw new IllegalArgumentException("Only enum state can declare enum values");
		}
		validateValue(type, minimum, maximum, enumValues, defaultValue);
	}

	public void validate(StateValue value) {
		validateValue(type, minimum, maximum, enumValues, value);
	}

	private static void validateValue(
		StateValueType type, Double minimum, Double maximum, Set<String> enumValues, StateValue value
	) {
		Objects.requireNonNull(value, "value");
		if (value.type() != type) throw new IllegalArgumentException("Expected " + type + " but got " + value.type());
		if (value instanceof StateValue.NumberValue number) {
			if ((minimum != null && number.value() < minimum) || (maximum != null && number.value() > maximum)) {
				throw new IllegalArgumentException("Number outside state range: " + number.value());
			}
		}
		if (value instanceof StateValue.EnumValue enumeration && !enumValues.contains(enumeration.value())) {
			throw new IllegalArgumentException("Unknown enum state value: " + enumeration.value());
		}
	}

	public static StatePropertyDefinition number(
		double defaultValue, Double minimum, Double maximum,
		StatePersistence persistence, StateDistribution distribution
	) {
		return new StatePropertyDefinition(StateValueType.NUMBER, StateValue.number(defaultValue), persistence,
			distribution, minimum, maximum, Set.of());
	}

	public static StatePropertyDefinition bool(
		boolean defaultValue, StatePersistence persistence, StateDistribution distribution
	) {
		return new StatePropertyDefinition(StateValueType.BOOLEAN, StateValue.bool(defaultValue), persistence,
			distribution, null, null, Set.of());
	}

	public static StatePropertyDefinition enumeration(
		String defaultValue, Set<String> values, StatePersistence persistence, StateDistribution distribution
	) {
		return new StatePropertyDefinition(StateValueType.ENUM, StateValue.enumeration(defaultValue), persistence,
			distribution, null, null, values);
	}
}
