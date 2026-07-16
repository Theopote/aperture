package dev.aperture.core.parametric;

import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.core.validation.ValidationIssue;

import java.util.List;
import java.util.Objects;

public record BooleanParameter(
	ParameterValue defaultValue,
	ParameterMetadata metadata
) implements Parameter {
	public BooleanParameter {
		Objects.requireNonNull(defaultValue, "defaultValue");
		Objects.requireNonNull(metadata, "metadata");
		if (defaultValue.type() != ParameterType.BOOL) {
			throw new IllegalArgumentException("Boolean parameter requires bool default");
		}
	}

	public static BooleanParameter of(boolean defaultValue) {
		return new BooleanParameter(ParameterValue.bool(defaultValue), ParameterMetadata.defaults());
	}

	public static BooleanParameter of(boolean defaultValue, ParameterMetadata metadata) {
		return new BooleanParameter(ParameterValue.bool(defaultValue), metadata);
	}

	@Override
	public ParameterKind kind() {
		return ParameterKind.BOOLEAN;
	}

	@Override
	public ParameterType storageType() {
		return ParameterType.BOOL;
	}

	@Override
	public void validateValue(String name, ParameterValue value, List<ValidationIssue> issues) {
		ParameterValidation.validateType(name, ParameterType.BOOL, value, issues);
	}
}
