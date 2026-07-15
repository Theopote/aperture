package dev.aperture.core.parametric;

import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.core.validation.ValidationIssue;

import java.util.List;
import java.util.OptionalDouble;

final class ParameterValidation {
	private ParameterValidation() {
	}

	static void validateType(String name, ParameterType expected, ParameterValue value, List<ValidationIssue> issues) {
		if (value.type() != expected) {
			issues.add(ValidationIssue.error(
				"parameter.type_mismatch",
				"Parameter " + name + " expected " + expected + " but got " + value.type()
			));
		}
	}

	static void validateNumericRange(
		String name,
		ParameterValue value,
		OptionalDouble min,
		OptionalDouble max,
		List<ValidationIssue> issues
	) {
		double numericValue = numericValue(value);
		if (Double.isNaN(numericValue)) {
			return;
		}

		min.ifPresent(minimum -> {
			if (numericValue < minimum) {
				issues.add(ValidationIssue.error(
					"parameter.below_min",
					"Parameter " + name + " value " + numericValue + " is below min " + minimum
				));
			}
		});

		max.ifPresent(maximum -> {
			if (numericValue > maximum) {
				issues.add(ValidationIssue.error(
					"parameter.above_max",
					"Parameter " + name + " value " + numericValue + " is above max " + maximum
				));
			}
		});
	}

	static void validateStep(
		String name,
		ParameterValue value,
		OptionalDouble step,
		List<ValidationIssue> issues
	) {
		if (step.isEmpty()) {
			return;
		}
		double numericValue = numericValue(value);
		if (Double.isNaN(numericValue)) {
			return;
		}
		double increment = step.getAsDouble();
		if (increment <= 0) {
			return;
		}
		double remainder = Math.abs(numericValue % increment);
		if (remainder > 1e-6 && Math.abs(remainder - increment) > 1e-6) {
			issues.add(ValidationIssue.error(
				"parameter.invalid_step",
				"Parameter " + name + " value " + numericValue + " does not align with step " + increment
			));
		}
	}

	static double numericValue(ParameterValue value) {
		return switch (value) {
			case ParameterValue.LengthValue length -> length.millimeters();
			case ParameterValue.AngleValue angle -> angle.degrees();
			case ParameterValue.CountValue count -> count.value();
			case ParameterValue.NumberValue number -> number.value();
			default -> Double.NaN;
		};
	}
}
