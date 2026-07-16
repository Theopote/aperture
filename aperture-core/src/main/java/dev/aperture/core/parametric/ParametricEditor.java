package dev.aperture.core.parametric;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.core.validation.ValidationIssue;
import dev.aperture.core.validation.ValidationResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Headless parameter editing API for UI editors, AI agents, and NodeCraft graphs.
 * Operates on parameter names and plain values — never on generator Java objects.
 */
public final class ParametricEditor {
	private final ParametricSchema schema;
	private final Map<String, ParameterValue> overrides;

	private ParametricEditor(ParametricSchema schema, Map<String, ParameterValue> overrides) {
		this.schema = schema;
		this.overrides = new LinkedHashMap<>(overrides);
	}

	public static ParametricEditor fromDefinition(OpeningTypeDefinition definition, ParameterSet overrides) {
		return new ParametricEditor(definition.parametricSchema(), overrides.asMap());
	}

	public static ParametricEditor fromInstance(OpeningTypeDefinition definition, OpeningInstance instance) {
		return fromDefinition(definition, instance.parameters());
	}

	public List<String> parameterNames() {
		return schema.names();
	}

	public Parameter describe(String name) {
		return schema.require(name);
	}

	public Optional<ParameterValue> override(String name) {
		return Optional.ofNullable(overrides.get(name));
	}

	public ParameterValue resolved(String name) {
		return overrides.getOrDefault(name, schema.require(name).defaultValue());
	}

	public Map<String, Object> snapshot() {
		Map<String, Object> values = new LinkedHashMap<>();
		for (String name : schema.names()) {
			values.put(name, ParameterBridge.toExternalValue(resolved(name)));
		}
		return values;
	}

	public ParametricEditResult set(String name, ParameterValue value) {
		Objects.requireNonNull(value, "value");
		if (schema.get(name).isEmpty()) {
			return ParametricEditResult.failed("parameter.unknown", "Unknown parameter: " + name);
		}

		List<ValidationIssue> issues = new ArrayList<>();
		Parameter parameter = schema.require(name);
		parameter.validateValue(name, value, issues);
		if (!issues.isEmpty()) {
			return ParametricEditResult.failed(issues);
		}

		overrides.put(name, value);
		return ParametricEditResult.ok(name);
	}

	public ParametricEditResult patch(Map<String, Object> externalValues) {
		Objects.requireNonNull(externalValues, "externalValues");
		ParametricEditResult.Builder result = ParametricEditResult.builder();

		for (Map.Entry<String, Object> entry : externalValues.entrySet()) {
			String name = entry.getKey();
			Optional<Parameter> parameter = schema.get(name);
			if (parameter.isEmpty()) {
				result.issue(ValidationIssue.error("parameter.unknown", "Unknown parameter: " + name));
				continue;
			}

			try {
				ParameterValue value = ParameterBridge.coerceExternalValue(parameter.get(), entry.getValue());
				List<ValidationIssue> issues = new ArrayList<>();
				parameter.get().validateValue(name, value, issues);
				if (!issues.isEmpty()) {
					result.issue(issues.getFirst());
					continue;
				}
				overrides.put(name, value);
				result.changed(name);
			} catch (RuntimeException exception) {
				result.issue(ValidationIssue.error("parameter.coerce_failed", exception.getMessage()));
			}
		}

		return result.build();
	}

	public ValidationResult validate() {
		List<ValidationIssue> issues = new ArrayList<>();
		for (String name : schema.names()) {
			Parameter parameter = schema.require(name);
			ParameterValue value = resolved(name);
			parameter.validateValue(name, value, issues);
		}
		return new ValidationResult(issues);
	}

	public ValidationResult validate(OpeningTypeDefinition definition) {
		ValidationResult schema = validate();
		ValidationResult constraints = new dev.aperture.core.constraint.ExpressionConstraintValidator()
			.validateResolved(definition, resolve());
		return schema.merge(constraints);
	}

	public ParameterSet resolve() {
		return schema.mergeDefaults(new ParameterSet(overrides));
	}

	public ParameterSet overridesOnly() {
		return new ParameterSet(overrides);
	}
}
