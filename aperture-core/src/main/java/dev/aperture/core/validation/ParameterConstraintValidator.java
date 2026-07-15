package dev.aperture.core.validation;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterDefinition;
import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates parameter values against the opening type schema (min/max, type match).
 */
public final class ParameterConstraintValidator implements OpeningValidator {
	@Override
	public ValidationResult validate(OpeningTypeDefinition definition, OpeningInstance instance) {
		List<ValidationIssue> issues = new ArrayList<>();

		for (Map.Entry<String, ParameterDefinition> entry : definition.parameters().entrySet()) {
			String name = entry.getKey();
			ParameterDefinition schema = entry.getValue();
			ParameterValue value = instance.parameters().asMap().get(name);

			if (value == null) {
				issues.add(ValidationIssue.error("parameter.missing", "Missing parameter: " + name));
				continue;
			}

			if (value.type() != schema.type()) {
				issues.add(ValidationIssue.error(
					"parameter.type_mismatch",
					"Parameter " + name + " expected " + schema.type() + " but got " + value.type()
				));
				continue;
			}

			validateRange(name, schema, value, issues);
		}

		return new ValidationResult(issues);
	}

	private static void validateRange(
		String name,
		ParameterDefinition schema,
		ParameterValue value,
		List<ValidationIssue> issues
	) {
		double numericValue = switch (value) {
			case ParameterValue.LengthValue length -> length.millimeters();
			case ParameterValue.AngleValue angle -> angle.degrees();
			case ParameterValue.CountValue count -> count.value();
			default -> Double.NaN;
		};

		if (Double.isNaN(numericValue)) {
			return;
		}

		schema.min().ifPresent(min -> {
			if (numericValue < min) {
				issues.add(ValidationIssue.error(
					"parameter.below_min",
					"Parameter " + name + " value " + numericValue + " is below min " + min
				));
			}
		});

		schema.max().ifPresent(max -> {
			if (numericValue > max) {
				issues.add(ValidationIssue.error(
					"parameter.above_max",
					"Parameter " + name + " value " + numericValue + " is above max " + max
				));
			}
		});

		if (schema.type() == ParameterType.ENUM && value instanceof ParameterValue.EnumValue enumValue) {
			if (!schema.enumValues().contains(enumValue.value())) {
				issues.add(ValidationIssue.error(
					"parameter.invalid_enum",
					"Parameter " + name + " value " + enumValue.value() + " is not in allowed set"
				));
			}
		}
	}
}
