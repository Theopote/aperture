package dev.aperture.core.parametric;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.core.validation.OpeningValidator;
import dev.aperture.core.validation.ValidationIssue;
import dev.aperture.core.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates resolved parameter values against a {@link ParametricSchema}.
 */
public final class ParametricValidator implements OpeningValidator {
	@Override
	public ValidationResult validate(OpeningTypeDefinition definition, OpeningInstance instance) {
		ParameterSet resolved = definition.parametricSchema().mergeDefaults(instance.parameters());
		List<ValidationIssue> issues = new ArrayList<>();

		for (String name : definition.parametricSchema().names()) {
			Parameter parameter = definition.parametricSchema().require(name);
			ParameterValue value = resolved.get(name).orElse(null);
			if (value == null) {
				issues.add(ValidationIssue.error("parameter.missing", "Missing parameter: " + name));
				continue;
			}
			parameter.validateValue(name, value, issues);
		}

		return new ValidationResult(issues);
	}
}
