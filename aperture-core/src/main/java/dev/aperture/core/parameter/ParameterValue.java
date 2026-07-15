package dev.aperture.core.parameter;

import dev.aperture.core.opening.OpeningId;

import java.util.Objects;

/**
 * Typed runtime value for a single opening parameter.
 */
public sealed interface ParameterValue permits
	ParameterValue.LengthValue,
	ParameterValue.AngleValue,
	ParameterValue.CountValue,
	ParameterValue.EnumValue,
	ParameterValue.BoolValue,
	ParameterValue.MaterialRefValue {

	static LengthValue length(double millimeters) {
		return new LengthValue(millimeters);
	}

	static AngleValue angle(double degrees) {
		return new AngleValue(degrees);
	}

	static CountValue count(int value) {
		return new CountValue(value);
	}

	static EnumValue enumValue(String value) {
		return new EnumValue(value);
	}

	static BoolValue bool(boolean value) {
		return new BoolValue(value);
	}

	static MaterialRefValue materialRef(String raw) {
		return new MaterialRefValue(raw);
	}

	ParameterType type();

	default void validateType(ParameterType expected) {
		if (type() != expected) {
			throw new IllegalArgumentException("Expected " + expected + " but got " + type());
		}
	}

	record LengthValue(double millimeters) implements ParameterValue {
		public LengthValue {
			if (millimeters < 0) {
				throw new IllegalArgumentException("Length must be non-negative");
			}
		}

		@Override
		public ParameterType type() {
			return ParameterType.LENGTH;
		}
	}

	record AngleValue(double degrees) implements ParameterValue {
		@Override
		public ParameterType type() {
			return ParameterType.ANGLE;
		}
	}

	record CountValue(int value) implements ParameterValue {
		public CountValue {
			if (value < 0) {
				throw new IllegalArgumentException("Count must be non-negative");
			}
		}

		@Override
		public ParameterType type() {
			return ParameterType.COUNT;
		}
	}

	record EnumValue(String value) implements ParameterValue {
		public EnumValue {
			Objects.requireNonNull(value, "value");
		}

		@Override
		public ParameterType type() {
			return ParameterType.ENUM;
		}
	}

	record BoolValue(boolean value) implements ParameterValue {
		@Override
		public ParameterType type() {
			return ParameterType.BOOL;
		}
	}

	record MaterialRefValue(String raw) implements ParameterValue {
		public MaterialRefValue {
			Objects.requireNonNull(raw, "raw");
		}

		public OpeningId asOpeningId() {
			return OpeningId.parse(raw);
		}

		@Override
		public ParameterType type() {
			return ParameterType.MATERIAL_REF;
		}
	}
}
