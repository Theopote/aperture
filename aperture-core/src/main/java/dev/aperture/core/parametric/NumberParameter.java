package dev.aperture.core.parametric;

import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.core.validation.ValidationIssue;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Free-form numeric parameter (length, angle, count, ratio, or unitless).
 */
public record NumberParameter(
	NumberUnit unit,
	ParameterValue defaultValue,
	OptionalDouble min,
	OptionalDouble max,
	OptionalDouble step,
	ParameterMetadata metadata
) implements Parameter {
	public NumberParameter {
		Objects.requireNonNull(unit, "unit");
		Objects.requireNonNull(defaultValue, "defaultValue");
		Objects.requireNonNull(min, "min");
		Objects.requireNonNull(max, "max");
		Objects.requireNonNull(step, "step");
		Objects.requireNonNull(metadata, "metadata");
		if (defaultValue.type() != unit.storageType()) {
			throw new IllegalArgumentException("Default value type mismatch for " + unit);
		}
	}

	public static Builder builder(NumberUnit unit) {
		return new Builder(unit);
	}

	@Override
	public ParameterKind kind() {
		return ParameterKind.NUMBER;
	}

	@Override
	public ParameterType storageType() {
		return unit.storageType();
	}

	@Override
	public void validateValue(String name, ParameterValue value, List<ValidationIssue> issues) {
		ParameterValidation.validateType(name, storageType(), value, issues);
		ParameterValidation.validateNumericRange(name, value, min, max, issues);
		ParameterValidation.validateStep(name, value, step, issues);
	}

	public static final class Builder {
		private final NumberUnit unit;
		private ParameterValue defaultValue;
		private OptionalDouble min = OptionalDouble.empty();
		private OptionalDouble max = OptionalDouble.empty();
		private OptionalDouble step = OptionalDouble.empty();
		private ParameterMetadata metadata = ParameterMetadata.defaults();

		private Builder(NumberUnit unit) {
			this.unit = unit;
		}

		public Builder defaultValue(ParameterValue value) {
			this.defaultValue = value;
			return this;
		}

		public Builder defaultNumber(double value) {
			return defaultValue(switch (unit) {
				case LENGTH_MM -> ParameterValue.length(value);
				case ANGLE_DEG -> ParameterValue.angle(value);
				case COUNT -> ParameterValue.count((int) Math.round(value));
				case RATIO, PLAIN -> ParameterValue.number(value);
			});
		}

		public Builder min(double value) {
			this.min = OptionalDouble.of(value);
			return this;
		}

		public Builder max(double value) {
			this.max = OptionalDouble.of(value);
			return this;
		}

		public Builder step(double value) {
			this.step = OptionalDouble.of(value);
			return this;
		}

		public Builder metadata(ParameterMetadata metadata) {
			this.metadata = metadata;
			return this;
		}

		public NumberParameter build() {
			if (defaultValue == null) {
				defaultValue = switch (unit) {
					case LENGTH_MM -> ParameterValue.length(0);
					case ANGLE_DEG -> ParameterValue.angle(0);
					case COUNT -> ParameterValue.count(0);
					case RATIO, PLAIN -> ParameterValue.number(0);
				};
			}
			return new NumberParameter(unit, defaultValue, min, max, step, metadata);
		}
	}
}
