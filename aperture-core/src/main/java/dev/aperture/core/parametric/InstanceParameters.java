package dev.aperture.core.parametric;

import com.google.gson.JsonObject;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.core.validation.ValidationResult;

import java.util.Map;
import java.util.Objects;

/**
 * Single entry point for reading and writing opening parameters.
 * <p>
 * External systems — GUI, NodeCraft, AI agents, JSON — must operate through this API
 * and {@link ParametricEditor}, never by mutating generator or component Java objects.
 * <p>
 * Storage contract: {@link OpeningInstance#parameters()} holds <em>sparse overrides</em>
 * only (values that differ from the type schema defaults). Use {@link #resolve} to obtain
 * the full effective {@link ParameterSet} for generation, rendering, and constraints.
 */
public final class InstanceParameters {
	private InstanceParameters() {
	}

	/**
	 * Resolves sparse overrides against the opening type schema.
	 */
	public static ParameterSet resolve(OpeningTypeDefinition definition, ParameterSet overrides) {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(overrides, "overrides");
		return definition.parametricSchema().mergeDefaults(overrides);
	}

	public static ParameterSet resolve(OpeningTypeDefinition definition, OpeningInstance instance) {
		return resolve(definition, instance.parameters());
	}

	/**
	 * Resolves parameters for geometry generation, applying runtime {@link OpeningState} when
	 * {@code open_angle} is not explicitly overridden on the instance.
	 */
	public static ParameterSet forGeneration(OpeningTypeDefinition definition, OpeningInstance instance) {
		ParameterSet resolved = resolve(definition, instance.parameters());
		if (instance.parameters().get("open_angle").isPresent()) {
			return resolved;
		}
		return OpeningStateParameters.apply(definition, resolved, instance.state());
	}

	/**
	 * Applies a JSON object patch (NodeCraft / AI) onto sparse overrides.
	 */
	public static ParametricEditResult patchJson(
		OpeningTypeDefinition definition,
		ParameterSet currentOverrides,
		JsonObject json
	) {
		return patch(definition, currentOverrides, ParameterSetJson.readPatchMap(json));
	}

	/**
	 * Strips schema defaults from a resolved or partially-resolved set, producing sparse overrides
	 * suitable for persistence on {@link OpeningInstance}.
	 */
	public static ParameterSet extractOverrides(OpeningTypeDefinition definition, ParameterSet values) {
		Objects.requireNonNull(definition, "definition");
		Objects.requireNonNull(values, "values");
		return definition.parametricSchema().extractOverrides(values);
	}

	/**
	 * Opens a headless editor bound to an instance's current overrides.
	 */
	public static ParametricEditor editor(OpeningTypeDefinition definition, OpeningInstance instance) {
		return ParametricEditor.fromInstance(definition, instance);
	}

	public static ParametricEditor editor(OpeningTypeDefinition definition, ParameterSet overrides) {
		return ParametricEditor.fromDefinition(definition, overrides);
	}

	/**
	 * Applies a patch from an external system (UI widget map, NodeCraft node output, AI JSON).
	 */
	public static ParametricEditResult patch(
		OpeningTypeDefinition definition,
		ParameterSet currentOverrides,
		Map<String, Object> externalValues
	) {
		return editor(definition, currentOverrides).patch(externalValues);
	}

	/**
	 * Full resolved snapshot as plain values for JSON export or UI display.
	 */
	public static Map<String, Object> snapshot(OpeningTypeDefinition definition, ParameterSet overrides) {
		return editor(definition, overrides).snapshot();
	}

	public static ValidationResult validate(OpeningTypeDefinition definition, ParameterSet overrides) {
		return editor(definition, overrides).validate(definition);
	}

	/**
	 * Writes validated overrides back onto an instance.
	 */
	public static OpeningInstance applyOverrides(
		OpeningTypeDefinition definition,
		OpeningInstance instance,
		ParameterSet overrides
	) {
		ParametricEditor editor = editor(definition, overrides);
		ValidationResult validation = editor.validate(definition);
		if (!validation.isValid()) {
			throw new IllegalArgumentException("Invalid parameter overrides: " + validation.issues());
		}
		return instance.withParameters(editor.overridesOnly());
	}

	/**
	 * Sets one parameter and returns a new instance with sparse overrides.
	 */
	public static OpeningInstance set(
		OpeningTypeDefinition definition,
		OpeningInstance instance,
		String name,
		ParameterValue value
	) {
		ParametricEditor editor = editor(definition, instance);
		ParametricEditResult result = editor.set(name, value);
		if (!result.success()) {
			throw new IllegalArgumentException("Failed to set " + name + ": " + result.issues());
		}
		ValidationResult validation = editor.validate(definition);
		if (!validation.isValid()) {
			throw new IllegalArgumentException("Invalid parameters after set: " + validation.issues());
		}
		return instance.withParameters(editor.overridesOnly()).withRevision(instance.revision() + 1);
	}
}
