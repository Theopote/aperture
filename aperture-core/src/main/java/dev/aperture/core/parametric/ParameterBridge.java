package dev.aperture.core.parametric;

import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;

import java.util.List;

/**
 * Converts external plain values for {@link ParametricEditor}, JSON, NodeCraft, and AI agents.
 */
public final class ParameterBridge {
	private ParameterBridge() {
	}

	public static NumberUnit unitForType(ParameterType type) {
		return switch (type) {
			case LENGTH -> NumberUnit.LENGTH_MM;
			case ANGLE -> NumberUnit.ANGLE_DEG;
			case COUNT -> NumberUnit.COUNT;
			case NUMBER -> NumberUnit.PLAIN;
			default -> throw new IllegalArgumentException("Not a numeric parameter type: " + type);
		};
	}

	public static ParameterValue coerceExternalValue(Parameter parameter, Object raw) {
		return switch (parameter) {
			case NumberParameter number -> coerceNumber(number.unit(), number.storageType(), raw);
			case RangeParameter range -> coerceNumber(range.unit(), range.storageType(), raw);
			case BooleanParameter ignored -> ParameterValue.bool(coerceBoolean(raw));
			case EnumParameter enumParameter -> ParameterValue.enumValue(coerceString(raw, enumParameter.values()));
			case ChoiceParameter choice -> ParameterValue.enumValue(coerceString(raw, choice.values()));
			case MaterialParameter ignored2 -> ParameterValue.materialRef(coerceString(raw, List.of()));
		};
	}

	private static ParameterValue coerceNumber(NumberUnit unit, ParameterType storageType, Object raw) {
		double value = coerceDouble(raw);
		return switch (storageType) {
			case LENGTH -> ParameterValue.length(value);
			case ANGLE -> ParameterValue.angle(value);
			case COUNT -> ParameterValue.count((int) Math.round(value));
			case NUMBER -> ParameterValue.number(value);
			default -> throw new IllegalArgumentException("Unsupported numeric storage type: " + storageType);
		};
	}

	private static double coerceDouble(Object raw) {
		return switch (raw) {
			case null -> throw new IllegalArgumentException("Numeric value cannot be null");
			case Number number -> number.doubleValue();
			case String string -> Double.parseDouble(string);
			default -> throw new IllegalArgumentException("Unsupported numeric value: " + raw);
		};
	}

	private static boolean coerceBoolean(Object raw) {
		return switch (raw) {
			case null -> throw new IllegalArgumentException("Boolean value cannot be null");
			case Boolean bool -> bool;
			case String string -> Boolean.parseBoolean(string);
			default -> throw new IllegalArgumentException("Unsupported boolean value: " + raw);
		};
	}

	private static String coerceString(Object raw, List<String> allowed) {
		if (raw == null) {
			throw new IllegalArgumentException("String value cannot be null");
		}
		String value = raw.toString();
		if (!allowed.isEmpty() && !allowed.contains(value)) {
			throw new IllegalArgumentException("Value " + value + " is not allowed");
		}
		return value;
	}

	public static Object toExternalValue(ParameterValue value) {
		return switch (value) {
			case ParameterValue.LengthValue length -> length.millimeters();
			case ParameterValue.AngleValue angle -> angle.degrees();
			case ParameterValue.CountValue count -> count.value();
			case ParameterValue.NumberValue number -> number.value();
			case ParameterValue.EnumValue enumValue -> enumValue.value();
			case ParameterValue.BoolValue bool -> bool.value();
			case ParameterValue.MaterialRefValue materialRef -> materialRef.raw();
		};
	}
}
