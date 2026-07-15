package dev.aperture.core.constraint;

import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.core.parametric.ParametricSchema;

/**
 * Evaluation context for constraint expressions against resolved parameter values.
 */
public record ConstraintContext(
	ParametricSchema schema,
	ParameterSet resolved
) {
	public double numeric(String name) {
		ParameterValue value = resolved.get(name)
			.orElseGet(() -> schema.require(name).defaultValue());
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
			.orElseGet(() -> schema.require(name).defaultValue());
		return switch (value) {
			case ParameterValue.EnumValue enumValue -> enumValue.value();
			case ParameterValue.MaterialRefValue materialRef -> materialRef.raw();
			case ParameterValue.BoolValue bool -> bool.value() ? "true" : "false";
			default -> throw new IllegalArgumentException("Parameter " + name + " is not textual");
		};
	}

	public boolean bool(String name) {
		ParameterValue value = resolved.get(name)
			.orElseGet(() -> schema.require(name).defaultValue());
		if (value instanceof ParameterValue.BoolValue bool) {
			return bool.value();
		}
		throw new IllegalArgumentException("Parameter " + name + " is not boolean");
	}
}
