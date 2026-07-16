package dev.aperture.core.parametric;

import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.core.validation.ValidationIssue;

import java.util.List;
import java.util.Objects;

/**
 * Closed string set without display labels.
 */
public record EnumParameter(
	List<String> values,
	ParameterValue defaultValue,
	ParameterMetadata metadata
) implements Parameter {
	public EnumParameter {
		values = List.copyOf(values);
		Objects.requireNonNull(defaultValue, "defaultValue");
		Objects.requireNonNull(metadata, "metadata");
		if (values.isEmpty()) {
			throw new IllegalArgumentException("Enum parameter requires values");
		}
		if (defaultValue.type() != ParameterType.ENUM) {
			throw new IllegalArgumentException("Enum parameter requires enum default");
		}
	}

	public static EnumParameter of(List<String> values, String defaultValue) {
		return new EnumParameter(values, ParameterValue.enumValue(defaultValue), ParameterMetadata.defaults());
	}

	@Override
	public ParameterKind kind() {
		return ParameterKind.ENUM;
	}

	@Override
	public ParameterType storageType() {
		return ParameterType.ENUM;
	}

	@Override
	public void validateValue(String name, ParameterValue value, List<ValidationIssue> issues) {
		ParameterValidation.validateType(name, ParameterType.ENUM, value, issues);
		if (value instanceof ParameterValue.EnumValue enumValue && !values.contains(enumValue.value())) {
			issues.add(ValidationIssue.error(
				"parameter.invalid_enum",
				"Parameter " + name + " value " + enumValue.value() + " is not in allowed set"
			));
		}
	}
}
