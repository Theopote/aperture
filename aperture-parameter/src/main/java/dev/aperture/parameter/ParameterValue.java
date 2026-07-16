package dev.aperture.parameter;

import java.util.Objects;

/**
 * Typed runtime value for a single opening parameter.
 */
public sealed interface ParameterValue permits
	ParameterValue.LengthValue,
	ParameterValue.AngleValue,
	ParameterValue.CountValue,
	ParameterValue.NumberValue,
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

	static NumberValue number(double value) {
		return new NumberValue(value);
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

	/** Extracts numeric value from LENGTH, ANGLE, COUNT, or NUMBER types. */
	default double asNumber() {
		return switch (this) {
			case LengthValue l -> l.millimeters();
			case AngleValue a -> a.degrees();
			case CountValue c -> (double) c.value();
			case NumberValue n -> n.value();
			default -> throw new IllegalStateException("Cannot convert " + type() + " to number");
		};
	}

	/** Extracts integer value from COUNT type. */
	default int asInt() {
		if (this instanceof CountValue c) {
			return c.value();
		}
		throw new IllegalStateException("Cannot convert " + type() + " to int");
	}

	/** Extracts boolean value from BOOL type. */
	default boolean asBoolean() {
		if (this instanceof BoolValue b) {
			return b.value();
		}
		throw new IllegalStateException("Cannot convert " + type() + " to boolean");
	}

	/** Extracts string value from ENUM or MATERIAL_REF types. */
	default String asString() {
		return switch (this) {
			case EnumValue e -> e.value();
			case MaterialRefValue m -> m.raw();
			default -> throw new IllegalStateException("Cannot convert " + type() + " to string");
		};
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

	record NumberValue(double value) implements ParameterValue {
		@Override
		public ParameterType type() {
			return ParameterType.NUMBER;
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

		@Override
		public ParameterType type() {
			return ParameterType.MATERIAL_REF;
		}
	}
}
