package dev.aperture.core.parametric;

import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.core.validation.ValidationIssue;

import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Numeric parameter with required min/max bounds and optional step — intended for sliders.
 */
public record RangeParameter(
	NumberUnit unit,
	ParameterValue defaultValue,
	double min,
	double max,
	OptionalDouble step,
	ParameterMetadata metadata
) implements Parameter {
	public RangeParameter {
		Objects.requireNonNull(unit, "unit");
		Objects.requireNonNull(defaultValue, "defaultValue");
		Objects.requireNonNull(step, "step");
		Objects.requireNonNull(metadata, "metadata");
		if (min > max) {
			throw new IllegalArgumentException("min must be <= max");
		}
		if (defaultValue.type() != unit.storageType()) {
			throw new IllegalArgumentException("Default value type mismatch for " + unit);
		}
	}

	public static Builder builder(NumberUnit unit) {
		return new Builder(unit);
	}

	@Override
	public ParameterKind kind() {
		return ParameterKind.RANGE;
	}

	@Override
	public ParameterType storageType() {
		return unit.storageType();
	}

	@Override
	public void validateValue(String name, ParameterValue value, List<ValidationIssue> issues) {
		ParameterValidation.validateType(name, storageType(), value, issues);
		ParameterValidation.validateNumericRange(name, value, OptionalDouble.of(min), OptionalDouble.of(max), issues);
		ParameterValidation.validateStep(name, value, step, issues);
	}

	public static final class Builder {
		private final NumberUnit unit;
		private ParameterValue defaultValue;
		private double min;
		private double max;
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
			this.min = value;
			return this;
		}

		public Builder max(double value) {
			this.max = value;
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

		public RangeParameter build() {
			if (defaultValue == null) {
				defaultValue = switch (unit) {
					case LENGTH_MM -> ParameterValue.length(min);
					case ANGLE_DEG -> ParameterValue.angle(min);
					case COUNT -> ParameterValue.count((int) Math.round(min));
					case RATIO, PLAIN -> ParameterValue.number(min);
				};
			}
			return new RangeParameter(unit, defaultValue, min, max, step, metadata);
		}
	}
}
