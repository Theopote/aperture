package dev.aperture.api.service;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parametric.ParametricEditor;
import dev.aperture.core.parametric.ParametricEditResult;
import dev.aperture.core.validation.ValidationResult;

import java.util.Map;

/**
 * Public API for editors, AI agents, and NodeCraft to manipulate opening parameters
 * without touching generator Java objects.
 */
public final class ParametricService {
	public ParametricEditor editor(OpeningTypeDefinition definition, ParameterSet overrides) {
		return ParametricEditor.fromDefinition(definition, overrides);
	}

	public ParametricEditor editor(OpeningTypeDefinition definition, OpeningInstance instance) {
		return ParametricEditor.fromInstance(definition, instance);
	}

	public Map<String, Object> snapshot(OpeningTypeDefinition definition, ParameterSet overrides) {
		return editor(definition, overrides).snapshot();
	}

	public ParametricEditResult patch(
		OpeningTypeDefinition definition,
		ParameterSet currentOverrides,
		Map<String, Object> values
	) {
		ParametricEditor parametricEditor = editor(definition, currentOverrides);
		return parametricEditor.patch(values);
	}

	public ValidationResult validate(OpeningTypeDefinition definition, ParameterSet overrides) {
		return editor(definition, overrides).validate(definition);
	}

	public ParameterSet resolve(OpeningTypeDefinition definition, ParameterSet overrides) {
		return editor(definition, overrides).resolve();
	}
}
