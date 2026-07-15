package dev.aperture.core.parametric;

import dev.aperture.core.parameter.ParameterDefinition;
import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Converts external values for {@link ParametricEditor} and legacy {@link ParameterDefinition} interop.
 */
public final class ParameterBridge {
	private ParameterBridge() {
	}

	/** @deprecated Use typed {@link Parameter} entries in {@link ParametricSchema}. */
	@Deprecated
	public static Parameter fromDefinition(ParameterDefinition definition) {
		return fromDefinition(definition, ParameterMetadata.defaults());
	}

	/** @deprecated Use typed {@link Parameter} entries in {@link ParametricSchema}. */
	@Deprecated
	public static Parameter fromDefinition(ParameterDefinition definition, ParameterMetadata metadata) {
		return switch (definition.type()) {
			case BOOL -> BooleanParameter.of(
				((ParameterValue.BoolValue) definition.defaultValue()).value(),
				metadata
			);
			case MATERIAL_REF -> MaterialParameter.of(
				((ParameterValue.MaterialRefValue) definition.defaultValue()).raw(),
				metadata
			);
			case ENUM -> new EnumParameter(definition.enumValues(), definition.defaultValue(), metadata);
			case LENGTH -> toNumeric(NumberUnit.LENGTH_MM, definition, metadata);
			case ANGLE -> toNumeric(NumberUnit.ANGLE_DEG, definition, metadata);
			case COUNT -> toNumeric(NumberUnit.COUNT, definition, metadata);
			case NUMBER -> toNumeric(NumberUnit.PLAIN, definition, metadata);
		};
	}

	private static Parameter toNumeric(NumberUnit unit, ParameterDefinition definition, ParameterMetadata metadata) {
		if (definition.min().isPresent() && definition.max().isPresent()) {
			RangeParameter.Builder builder = RangeParameter.builder(unit)
				.defaultValue(definition.defaultValue())
				.min(definition.min().getAsDouble())
				.max(definition.max().getAsDouble())
				.metadata(metadata);
			definition.step().ifPresent(builder::step);
			return builder.build();
		}

		NumberParameter.Builder builder = NumberParameter.builder(unit)
			.defaultValue(definition.defaultValue())
			.metadata(metadata);
		definition.min().ifPresent(builder::min);
		definition.max().ifPresent(builder::max);
		definition.step().ifPresent(builder::step);
		return builder.build();
	}

	/** @deprecated Use {@link ParametricSchema#parameters()}. */
	@Deprecated
	public static Map<String, ParameterDefinition> toLegacyMap(Map<String, Parameter> parameters) {
		Map<String, ParameterDefinition> legacy = new LinkedHashMap<>();
		for (Map.Entry<String, Parameter> entry : parameters.entrySet()) {
			legacy.put(entry.getKey(), entry.getValue().toDefinition());
		}
		return legacy;
	}

	/** @deprecated Use {@link ParametricSchema#builder()}. */
	@Deprecated
	public static Map<String, Parameter> fromLegacyMap(Map<String, ParameterDefinition> legacy) {
		Map<String, Parameter> parameters = new LinkedHashMap<>();
		for (Map.Entry<String, ParameterDefinition> entry : legacy.entrySet()) {
			parameters.put(entry.getKey(), fromDefinition(entry.getValue()));
		}
		return parameters;
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
