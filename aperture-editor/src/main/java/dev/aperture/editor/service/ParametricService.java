package dev.aperture.editor.service;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parametric.InstanceParameters;
import dev.aperture.core.parametric.ParametricEditor;
import dev.aperture.core.parametric.ParametricEditResult;
import dev.aperture.core.validation.ValidationResult;

import java.util.Map;

/**
 * Public API for editors, AI agents, and NodeCraft to manipulate opening parameters
 * without touching generator Java objects. Delegates to {@link InstanceParameters}.
 */
public final class ParametricService {
	public ParametricEditor editor(OpeningTypeDefinition definition, ParameterSet overrides) {
		return InstanceParameters.editor(definition, overrides);
	}

	public ParametricEditor editor(OpeningTypeDefinition definition, OpeningInstance instance) {
		return InstanceParameters.editor(definition, instance);
	}

	public Map<String, Object> snapshot(OpeningTypeDefinition definition, ParameterSet overrides) {
		return InstanceParameters.snapshot(definition, overrides);
	}

	public ParametricEditResult patch(
		OpeningTypeDefinition definition,
		ParameterSet currentOverrides,
		Map<String, Object> values
	) {
		return InstanceParameters.patch(definition, currentOverrides, values);
	}

	public ValidationResult validate(OpeningTypeDefinition definition, ParameterSet overrides) {
		return InstanceParameters.validate(definition, overrides);
	}

	public ParameterSet resolve(OpeningTypeDefinition definition, ParameterSet overrides) {
		return InstanceParameters.resolve(definition, overrides);
	}
}
