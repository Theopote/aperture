package dev.aperture.core.parametric;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON helpers for {@link ParameterSet} values against a {@link ParametricSchema}.
 * Used by instance codecs, NodeCraft, and AI agents.
 */
public final class ParameterSetJson {
	private ParameterSetJson() {
	}

	public static JsonObject writeOverrides(ParameterSet overrides) {
		JsonObject object = new JsonObject();
		for (Map.Entry<String, ParameterValue> entry : overrides.asMap().entrySet()) {
			object.add(entry.getKey(), writeValue(entry.getValue()));
		}
		return object;
	}

	public static JsonObject writeResolved(OpeningTypeDefinition definition, ParameterSet overrides) {
		Map<String, Object> snapshot = InstanceParameters.snapshot(definition, overrides);
		JsonObject object = new JsonObject();
		for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
			object.add(entry.getKey(), toJsonElement(entry.getValue()));
		}
		return object;
	}

	public static ParameterSet readOverrides(ParametricSchema schema, JsonObject object) {
		ParameterSet.Builder builder = ParameterSet.builder();
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			Parameter parameter = schema.require(entry.getKey());
			Object raw = fromJsonElement(entry.getValue());
			builder.put(entry.getKey(), ParameterBridge.coerceExternalValue(parameter, raw));
		}
		return builder.build();
	}

	public static Map<String, Object> readPatchMap(JsonObject object) {
		Map<String, Object> values = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			values.put(entry.getKey(), fromJsonElement(entry.getValue()));
		}
		return values;
	}

	private static JsonElement writeValue(ParameterValue value) {
		return toJsonElement(ParameterBridge.toExternalValue(value));
	}

	private static JsonElement toJsonElement(Object value) {
		if (value instanceof Boolean bool) {
			return new com.google.gson.JsonPrimitive(bool);
		}
		if (value instanceof Number number) {
			return new com.google.gson.JsonPrimitive(number);
		}
		return new com.google.gson.JsonPrimitive(String.valueOf(value));
	}

	private static Object fromJsonElement(JsonElement element) {
		if (element.isJsonNull()) {
			return null;
		}
		if (element.isJsonPrimitive()) {
			var primitive = element.getAsJsonPrimitive();
			if (primitive.isBoolean()) {
				return primitive.getAsBoolean();
			}
			if (primitive.isString()) {
				return primitive.getAsString();
			}
			if (primitive.isNumber()) {
				return primitive.getAsNumber();
			}
		}
		throw new IllegalArgumentException("Unsupported JSON value: " + element);
	}
}
