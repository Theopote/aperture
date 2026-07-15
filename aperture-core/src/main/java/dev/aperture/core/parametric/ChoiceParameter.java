package dev.aperture.core.parametric;

import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.core.validation.ValidationIssue;

import java.util.List;
import java.util.Objects;

/**
 * Labeled option set for UI pickers and AI tool calls.
 */
public record ChoiceParameter(
	List<ChoiceOption> choices,
	ParameterValue defaultValue,
	ParameterMetadata metadata
) implements Parameter {
	public ChoiceParameter {
		choices = List.copyOf(choices);
		Objects.requireNonNull(defaultValue, "defaultValue");
		Objects.requireNonNull(metadata, "metadata");
		if (choices.isEmpty()) {
			throw new IllegalArgumentException("Choice parameter requires choices");
		}
		if (defaultValue.type() != ParameterType.ENUM) {
			throw new IllegalArgumentException("Choice parameter stores enum values");
		}
	}

	public static ChoiceParameter of(List<ChoiceOption> choices, String defaultValue) {
		return new ChoiceParameter(choices, ParameterValue.enumValue(defaultValue), ParameterMetadata.defaults());
	}

	public List<String> values() {
		return choices.stream().map(ChoiceOption::value).toList();
	}

	@Override
	public ParameterKind kind() {
		return ParameterKind.CHOICE;
	}

	@Override
	public ParameterType storageType() {
		return ParameterType.ENUM;
	}

	@Override
	public void validateValue(String name, ParameterValue value, List<ValidationIssue> issues) {
		ParameterValidation.validateType(name, ParameterType.ENUM, value, issues);
		if (value instanceof ParameterValue.EnumValue enumValue && !values().contains(enumValue.value())) {
			issues.add(ValidationIssue.error(
				"parameter.invalid_choice",
				"Parameter " + name + " value " + enumValue.value() + " is not a valid choice"
			));
		}
	}
}
