package dev.aperture.core.parametric;

import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.core.validation.ValidationIssue;

import java.util.List;
import java.util.Objects;

public record MaterialParameter(
	ParameterValue defaultValue,
	ParameterMetadata metadata
) implements Parameter {
	public MaterialParameter {
		Objects.requireNonNull(defaultValue, "defaultValue");
		Objects.requireNonNull(metadata, "metadata");
		if (defaultValue.type() != ParameterType.MATERIAL_REF) {
			throw new IllegalArgumentException("Material parameter requires material_ref default");
		}
	}

	public static MaterialParameter of(String defaultMaterial) {
		return new MaterialParameter(ParameterValue.materialRef(defaultMaterial), ParameterMetadata.defaults());
	}

	public static MaterialParameter of(String defaultMaterial, ParameterMetadata metadata) {
		return new MaterialParameter(ParameterValue.materialRef(defaultMaterial), metadata);
	}

	@Override
	public ParameterKind kind() {
		return ParameterKind.MATERIAL;
	}

	@Override
	public ParameterType storageType() {
		return ParameterType.MATERIAL_REF;
	}

	@Override
	public void validateValue(String name, ParameterValue value, List<ValidationIssue> issues) {
		ParameterValidation.validateType(name, ParameterType.MATERIAL_REF, value, issues);
	}
}
