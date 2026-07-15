package dev.aperture.core.parameter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Schema entry describing a single parametric dimension on an opening type.
 */
public record ParameterDefinition(
	ParameterType type,
	ParameterValue defaultValue,
	OptionalDouble min,
	OptionalDouble max,
	List<String> enumValues
) {
	public ParameterDefinition {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(defaultValue, "defaultValue");
		Objects.requireNonNull(min, "min");
		Objects.requireNonNull(max, "max");
		enumValues = List.copyOf(enumValues);
		defaultValue.validateType(type);

		if (type == ParameterType.ENUM && enumValues.isEmpty()) {
			throw new IllegalArgumentException("Enum parameter requires values");
		}
	}

	public static Builder builder(ParameterType type) {
		return new Builder(type);
	}

	public OptionalInt minAsInt() {
		return min.stream().mapToInt(v -> (int) Math.round(v)).findFirst();
	}

	public OptionalInt maxAsInt() {
		return max.stream().mapToInt(v -> (int) Math.round(v)).findFirst();
	}

	public static final class Builder {
		private final ParameterType type;
		private ParameterValue defaultValue;
		private OptionalDouble min = OptionalDouble.empty();
		private OptionalDouble max = OptionalDouble.empty();
		private List<String> enumValues = List.of();

		private Builder(ParameterType type) {
			this.type = type;
		}

		public Builder defaultValue(ParameterValue value) {
			this.defaultValue = value;
			return this;
		}

		public Builder min(double value) {
			this.min = OptionalDouble.of(value);
			return this;
		}

		public Builder max(double value) {
			this.max = OptionalDouble.of(value);
			return this;
		}

		public Builder enumValues(String... values) {
			this.enumValues = List.of(values);
			return this;
		}

		public ParameterDefinition build() {
			if (defaultValue == null) {
				defaultValue = switch (type) {
					case LENGTH -> ParameterValue.length(0);
					case ANGLE -> ParameterValue.angle(0);
					case COUNT -> ParameterValue.count(0);
					case BOOL -> ParameterValue.bool(false);
					case ENUM -> ParameterValue.enumValue(enumValues.isEmpty() ? "" : enumValues.getFirst());
					case MATERIAL_REF -> ParameterValue.materialRef("");
				};
			}
			return new ParameterDefinition(type, defaultValue, min, max, enumValues);
		}
	}
}
