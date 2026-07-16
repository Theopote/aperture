package dev.aperture.constraint;

import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import java.util.function.Function;

/**
 * Evaluation context for constraint expressions against resolved parameter values.
 */
public record ConstraintContext(
		Function<String, ParameterValue> defaults,
		ParameterSet resolved
) {
	public double numeric(String name) {
		ParameterValue value = resolved.get(name)
				.orElseGet(() -> defaults.apply(name));
		return switch (value) {
			case ParameterValue.LengthValue length -> length.millimeters();
			case ParameterValue.AngleValue angle -> angle.degrees();
			case ParameterValue.CountValue count -> count.value();
			case ParameterValue.NumberValue number -> number.value();
			case ParameterValue.BoolValue bool -> bool.value() ? 1.0 : 0.0;
			default -> throw new IllegalArgumentException("Parameter " + name + " is not numeric");
		};
	}

	public String text(String name) {
		ParameterValue value = resolved.get(name)
				.orElseGet(() -> defaults.apply(name));
		return switch (value) {
			case ParameterValue.EnumValue enumValue -> enumValue.value();
			case ParameterValue.MaterialRefValue materialRef -> materialRef.raw();
			case ParameterValue.BoolValue bool -> bool.value() ? "true" : "false";
			default -> throw new IllegalArgumentException("Parameter " + name + " is not textual");
		};
	}

	public boolean bool(String name) {
		ParameterValue value = resolved.get(name)
				.orElseGet(() -> defaults.apply(name));
		if (value instanceof ParameterValue.BoolValue bool) {
			return bool.value();
		}
		throw new IllegalArgumentException("Parameter " + name + " is not boolean");
	}
}
