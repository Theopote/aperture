package dev.aperture.core.parameter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolved parameter values for an opening instance or generation request.
 * <p>
 * Instances store <em>sparse overrides</em> (see {@link dev.aperture.core.parametric.InstanceParameters}).
 * Use {@link #mergeDefaults} or {@link dev.aperture.core.definition.OpeningTypeDefinition#resolveParameters}
 * to obtain the full effective set before generation or validation.
 */
public final class ParameterSet {
	private final Map<String, ParameterValue> values;

	public ParameterSet(Map<String, ParameterValue> values) {
		this.values = Map.copyOf(values);
	}

	public static ParameterSet empty() {
		return new ParameterSet(Map.of());
	}

	public static ParameterSet of(String key, ParameterValue value) {
		return new ParameterSet(Map.of(key, value));
	}

	public static Builder builder() {
		return new Builder();
	}

	public static ParameterSet mergeDefaults(Map<String, ParameterDefinition> schema, ParameterSet overrides) {
		Builder builder = builder();
		for (Map.Entry<String, ParameterDefinition> entry : schema.entrySet()) {
			builder.put(entry.getKey(), entry.getValue().defaultValue());
		}
		builder.putAll(overrides.values);
		return builder.build();
	}

	public Map<String, ParameterValue> asMap() {
		return values;
	}

	public Optional<ParameterValue> get(String name) {
		return Optional.ofNullable(values.get(name));
	}

	public double requireLength(String name) {
		return ((ParameterValue.LengthValue) require(name, ParameterType.LENGTH)).millimeters();
	}

	public int requireCount(String name) {
		return ((ParameterValue.CountValue) require(name, ParameterType.COUNT)).value();
	}

	public double requireAngle(String name) {
		return ((ParameterValue.AngleValue) require(name, ParameterType.ANGLE)).degrees();
	}

	public double requireNumber(String name) {
		return ((ParameterValue.NumberValue) require(name, ParameterType.NUMBER)).value();
	}

	public boolean requireBool(String name) {
		return ((ParameterValue.BoolValue) require(name, ParameterType.BOOL)).value();
	}

	public String requireEnum(String name) {
		return ((ParameterValue.EnumValue) require(name, ParameterType.ENUM)).value();
	}

	public String requireMaterialRef(String name) {
		return ((ParameterValue.MaterialRefValue) require(name, ParameterType.MATERIAL_REF)).raw();
	}

	private ParameterValue require(String name, ParameterType type) {
		ParameterValue value = values.get(name);
		if (value == null) {
			throw new IllegalArgumentException("Missing parameter: " + name);
		}
		value.validateType(type);
		return value;
	}

	public static final class Builder {
		private final Map<String, ParameterValue> values = new LinkedHashMap<>();

		public Builder put(String name, ParameterValue value) {
			values.put(name, value);
			return this;
		}

		public Builder putAll(Map<String, ParameterValue> other) {
			values.putAll(other);
			return this;
		}

		public ParameterSet build() {
			return new ParameterSet(Collections.unmodifiableMap(new LinkedHashMap<>(values)));
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ParameterSet other && values.equals(other.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(values);
	}

	@Override
	public String toString() {
		return values.toString();
	}
}
