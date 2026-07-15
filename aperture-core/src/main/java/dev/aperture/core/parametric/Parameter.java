package dev.aperture.core.parametric;

import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.core.validation.ValidationIssue;

import java.util.List;

/**
 * Schema for one parametric dimension on an opening type.
 * Editors, AI agents, and NodeCraft operate on {@link Parameter} metadata and plain values —
 * not on generator Java objects.
 */
public sealed interface Parameter permits
	NumberParameter,
	RangeParameter,
	BooleanParameter,
	ChoiceParameter,
	EnumParameter,
	MaterialParameter {

	ParameterKind kind();

	ParameterMetadata metadata();

	ParameterValue defaultValue();

	ParameterType storageType();

	void validateValue(String name, ParameterValue value, List<ValidationIssue> issues);

	default String label() {
		return metadata().label();
	}

	default String group() {
		return metadata().group();
	}
}
