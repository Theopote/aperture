package dev.aperture.core.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aperture.core.definition.ConstraintRule;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.opening.GeneratorId;
import dev.aperture.core.opening.OpeningCategory;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parameter.ParameterDefinition;
import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads {@link OpeningTypeDefinition} from JSON data packs.
 */
public final class OpeningTypeDefinitionReader {
	public OpeningTypeDefinition read(Path path) throws IOException {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return parse(JsonParser.parseReader(reader).getAsJsonObject());
		}
	}

	public OpeningTypeDefinition read(InputStream inputStream) throws IOException {
		try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			return parse(JsonParser.parseReader(reader).getAsJsonObject());
		}
	}

	public OpeningTypeDefinition parse(JsonObject root) {
		int schemaVersion = root.get("schemaVersion").getAsInt();
		OpeningId id = OpeningId.parse(root.get("id").getAsString());
		OpeningCategory category = OpeningCategory.fromId(root.get("category").getAsString());
		GeneratorId generator = GeneratorId.parse(root.get("generator").getAsString());

		Map<String, ParameterDefinition> parameters = new LinkedHashMap<>();
		if (root.has("parameters")) {
			JsonObject parametersObject = root.getAsJsonObject("parameters");
			for (Map.Entry<String, JsonElement> entry : parametersObject.entrySet()) {
				parameters.put(entry.getKey(), parseParameter(entry.getValue().getAsJsonObject()));
			}
		}

		List<ConstraintRule> constraints = new ArrayList<>();
		if (root.has("constraints")) {
			JsonArray constraintsArray = root.getAsJsonArray("constraints");
			for (JsonElement element : constraintsArray) {
				JsonObject constraint = element.getAsJsonObject();
				constraints.add(new ConstraintRule(
					constraint.get("expr").getAsString(),
					constraint.get("message").getAsString()
				));
			}
		}

		Map<String, Object> components = new LinkedHashMap<>();
		if (root.has("components")) {
			components.putAll(parseComponents(root.getAsJsonObject("components")));
		}

		List<String> materialSlots = new ArrayList<>();
		if (root.has("materialSlots")) {
			for (JsonElement element : root.getAsJsonArray("materialSlots")) {
				materialSlots.add(element.getAsString());
			}
		}

		return new OpeningTypeDefinition(
			schemaVersion,
			id,
			category,
			parameters,
			constraints,
			generator,
			components,
			materialSlots
		);
	}

	private static ParameterDefinition parseParameter(JsonObject object) {
		ParameterType type = parseParameterType(object.get("type").getAsString());
		ParameterDefinition.Builder builder = ParameterDefinition.builder(type);

		if (object.has("default")) {
			builder.defaultValue(parseParameterValue(type, object.get("default")));
		}
		if (object.has("min")) {
			builder.min(object.get("min").getAsDouble());
		}
		if (object.has("max")) {
			builder.max(object.get("max").getAsDouble());
		}
		if (object.has("values")) {
			List<String> values = new ArrayList<>();
			for (JsonElement element : object.getAsJsonArray("values")) {
				values.add(element.getAsString());
			}
			builder.enumValues(values.toArray(String[]::new));
		}

		return builder.build();
	}

	private static ParameterType parseParameterType(String raw) {
		return switch (raw) {
			case "length" -> ParameterType.LENGTH;
			case "angle" -> ParameterType.ANGLE;
			case "count" -> ParameterType.COUNT;
			case "enum" -> ParameterType.ENUM;
			case "bool" -> ParameterType.BOOL;
			case "material_ref" -> ParameterType.MATERIAL_REF;
			default -> throw new IllegalArgumentException("Unknown parameter type: " + raw);
		};
	}

	private static ParameterValue parseParameterValue(ParameterType type, JsonElement element) {
		return switch (type) {
			case LENGTH -> ParameterValue.length(element.getAsDouble());
			case ANGLE -> ParameterValue.angle(element.getAsDouble());
			case COUNT -> ParameterValue.count(element.getAsInt());
			case ENUM -> ParameterValue.enumValue(element.getAsString());
			case BOOL -> ParameterValue.bool(element.getAsBoolean());
			case MATERIAL_REF -> ParameterValue.materialRef(element.getAsString());
		};
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> parseComponents(JsonObject object) {
		Map<String, Object> components = new LinkedHashMap<>();
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			if (entry.getValue().isJsonObject()) {
				Map<String, String> nested = new LinkedHashMap<>();
				for (Map.Entry<String, JsonElement> nestedEntry : entry.getValue().getAsJsonObject().entrySet()) {
					nested.put(nestedEntry.getKey(), nestedEntry.getValue().getAsString());
				}
				components.put(entry.getKey(), nested);
			} else {
				components.put(entry.getKey(), entry.getValue().getAsString());
			}
		}
		return components;
	}
}
