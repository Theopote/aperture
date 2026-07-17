package dev.aperture.core.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.aperture.core.state.RuntimeState;
import dev.aperture.core.state.StatePropertyDefinition;
import dev.aperture.core.state.StatePropertyType;
import dev.aperture.core.state.StateSchema;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** JSON mapping for state schemas and persistent runtime-state snapshots. */
public final class RuntimeStateJson {
	private RuntimeStateJson() { }

	public static StateSchema readSchema(JsonObject object) {
		StateSchema.Builder schema = StateSchema.builder();
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			JsonObject property = entry.getValue().getAsJsonObject();
			StatePropertyType type = parseType(property.get("type").getAsString());
			boolean persistent = !property.has("persistent") || property.get("persistent").getAsBoolean();
			Object defaultValue = property.has("default")
				? readValue(type, property.get("default"))
				: defaultValue(type, property);
			Double min = property.has("min") ? property.get("min").getAsDouble() : null;
			Double max = property.has("max") ? property.get("max").getAsDouble() : null;
			Set<String> values = new LinkedHashSet<>();
			if (property.has("values")) {
				property.getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
			}
			schema.property(entry.getKey(), new StatePropertyDefinition(
				type, defaultValue, min, max, values, persistent
			));
		}
		return schema.build();
	}

	public static JsonObject writePersistent(RuntimeState state) {
		JsonObject object = new JsonObject();
		JsonObject persistent = new JsonObject();
		state.persistentProperties().forEach((name, value) -> persistent.add(name, writeValue(value)));
		object.add("persistent", persistent);
		object.addProperty("revision", state.revision());
		object.addProperty("timestamp", state.timestamp().toString());
		return object;
	}

	public static RuntimeState readPersistent(StateSchema schema, JsonObject object) {
		Map<String, Object> persistent = new LinkedHashMap<>();
		if (object.has("persistent")) {
			for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("persistent").entrySet()) {
				persistent.put(entry.getKey(), readValue(schema.require(entry.getKey()).type(), entry.getValue()));
			}
		} else if (object.has("openRatio") && schema.properties().containsKey("openRatio")) {
			persistent.put("openRatio", object.get("openRatio").getAsDouble());
		}
		long revision = object.has("revision") ? object.get("revision").getAsLong() : 0;
		Instant timestamp = object.has("timestamp") ? Instant.parse(object.get("timestamp").getAsString()) : Instant.EPOCH;
		return RuntimeState.restore(schema, persistent, revision, timestamp);
	}

	private static StatePropertyType parseType(String raw) {
		return switch (raw) {
			case "number" -> StatePropertyType.NUMBER;
			case "boolean", "bool" -> StatePropertyType.BOOLEAN;
			case "string" -> StatePropertyType.STRING;
			case "enum" -> StatePropertyType.ENUM;
			default -> throw new IllegalArgumentException("Unknown state property type: " + raw);
		};
	}

	private static Object defaultValue(StatePropertyType type, JsonObject property) {
		return switch (type) {
			case NUMBER -> 0.0;
			case BOOLEAN -> false;
			case STRING -> "";
			case ENUM -> property.getAsJsonArray("values").get(0).getAsString();
		};
	}

	private static Object readValue(StatePropertyType type, JsonElement value) {
		return switch (type) {
			case NUMBER -> value.getAsDouble();
			case BOOLEAN -> value.getAsBoolean();
			case STRING, ENUM -> value.getAsString();
		};
	}

	private static JsonElement writeValue(Object value) {
		if (value instanceof Number number) return new JsonPrimitive(number);
		if (value instanceof Boolean bool) return new JsonPrimitive(bool);
		return new JsonPrimitive(String.valueOf(value));
	}
}
