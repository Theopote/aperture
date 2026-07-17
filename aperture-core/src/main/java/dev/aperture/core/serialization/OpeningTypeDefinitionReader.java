package dev.aperture.core.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aperture.core.component.ComponentAssembly;
import dev.aperture.core.definition.ConstraintRule;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.opening.GeneratorId;
import dev.aperture.core.opening.OpeningCategory;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parametric.BooleanParameter;
import dev.aperture.core.parametric.ChoiceOption;
import dev.aperture.core.parametric.ChoiceParameter;
import dev.aperture.core.parametric.EnumParameter;
import dev.aperture.core.parametric.MaterialParameter;
import dev.aperture.core.parametric.NumberParameter;
import dev.aperture.core.parametric.NumberUnit;
import dev.aperture.core.parametric.Parameter;
import dev.aperture.core.parametric.ParameterKind;
import dev.aperture.core.parametric.ParameterMetadata;
import dev.aperture.core.parametric.ParametricSchema;
import dev.aperture.core.parametric.RangeParameter;
import dev.aperture.core.state.StateSchema;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;

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
	private final ComponentAssemblyReader componentReader = new ComponentAssemblyReader();

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

		StateSchema stateSchema = root.has("stateSchema")
			? RuntimeStateJson.readSchema(root.getAsJsonObject("stateSchema"))
			: StateSchema.empty();

		ParametricSchema.Builder parameters = ParametricSchema.builder();
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

		ComponentAssembly components = root.has("components")
			? componentReader.read(root.get("components"))
			: ComponentAssembly.empty();

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
			parameters.build(),
			stateSchema,
			constraints,
			generator,
			components,
			materialSlots
		);
	}

	private static Parameter parseParameter(JsonObject object) {
		ParameterMetadata metadata = parseMetadata(object);
		ParameterKind kind = object.has("kind")
			? parseKind(object.get("kind").getAsString())
			: inferKind(object);

		return switch (kind) {
			case BOOLEAN -> BooleanParameter.of(
				object.has("default") && object.get("default").getAsBoolean(),
				metadata
			);
			case MATERIAL -> MaterialParameter.of(
				object.has("default") ? object.get("default").getAsString() : "",
				metadata
			);
			case ENUM -> new EnumParameter(
				readStringList(object, "values"),
				object.has("default")
					? ParameterValue.enumValue(object.get("default").getAsString())
					: ParameterValue.enumValue(readStringList(object, "values").getFirst()),
				metadata
			);
			case CHOICE -> new ChoiceParameter(
				readChoices(object),
				object.has("default")
					? ParameterValue.enumValue(object.get("default").getAsString())
					: ParameterValue.enumValue(readChoices(object).getFirst().value()),
				metadata
			);
			case RANGE -> parseRange(object, metadata);
			case NUMBER -> parseNumber(object, metadata);
		};
	}

	private static Parameter parseRange(JsonObject object, ParameterMetadata metadata) {
		NumberUnit unit = unitFromJson(object);
		RangeParameter.Builder builder = RangeParameter.builder(unit)
			.min(object.get("min").getAsDouble())
			.max(object.get("max").getAsDouble())
			.metadata(metadata);
		if (object.has("default")) {
			builder.defaultValue(parseParameterValue(unit.storageType(), object.get("default")));
		}
		if (object.has("step")) {
			builder.step(object.get("step").getAsDouble());
		}
		return builder.build();
	}

	private static Parameter parseNumber(JsonObject object, ParameterMetadata metadata) {
		NumberUnit unit = unitFromJson(object);
		NumberParameter.Builder builder = NumberParameter.builder(unit).metadata(metadata);
		if (object.has("default")) {
			builder.defaultValue(parseParameterValue(unit.storageType(), object.get("default")));
		}
		if (object.has("min")) {
			builder.min(object.get("min").getAsDouble());
		}
		if (object.has("max")) {
			builder.max(object.get("max").getAsDouble());
		}
		if (object.has("step")) {
			builder.step(object.get("step").getAsDouble());
		}
		return builder.build();
	}

	private static ParameterKind inferKind(JsonObject object) {
		String type = object.get("type").getAsString();
		return switch (type) {
			case "bool" -> ParameterKind.BOOLEAN;
			case "material_ref" -> ParameterKind.MATERIAL;
			case "enum" -> object.has("choices") ? ParameterKind.CHOICE : ParameterKind.ENUM;
			default -> object.has("min") && object.has("max") ? ParameterKind.RANGE : ParameterKind.NUMBER;
		};
	}

	private static ParameterKind parseKind(String raw) {
		return switch (raw) {
			case "number" -> ParameterKind.NUMBER;
			case "range" -> ParameterKind.RANGE;
			case "boolean", "bool" -> ParameterKind.BOOLEAN;
			case "choice" -> ParameterKind.CHOICE;
			case "enum" -> ParameterKind.ENUM;
			case "material", "material_ref" -> ParameterKind.MATERIAL;
			default -> throw new IllegalArgumentException("Unknown parameter kind: " + raw);
		};
	}

	private static NumberUnit unitFromJson(JsonObject object) {
		ParameterType type = parseParameterType(object.get("type").getAsString());
		return switch (type) {
			case LENGTH -> NumberUnit.LENGTH_MM;
			case ANGLE -> NumberUnit.ANGLE_DEG;
			case COUNT -> NumberUnit.COUNT;
			case NUMBER -> NumberUnit.PLAIN;
			default -> throw new IllegalArgumentException("Not a numeric parameter type: " + type);
		};
	}

	private static ParameterMetadata parseMetadata(JsonObject object) {
		String label = object.has("label") ? object.get("label").getAsString() : "";
		String group = object.has("group") ? object.get("group").getAsString() : "";
		String description = object.has("description") ? object.get("description").getAsString() : "";
		boolean readOnly = object.has("readOnly") && object.get("readOnly").getAsBoolean();
		return new ParameterMetadata(label, group, description, readOnly);
	}

	private static List<String> readStringList(JsonObject object, String key) {
		List<String> values = new ArrayList<>();
		for (JsonElement element : object.getAsJsonArray(key)) {
			values.add(element.getAsString());
		}
		return values;
	}

	private static List<ChoiceOption> readChoices(JsonObject object) {
		List<ChoiceOption> choices = new ArrayList<>();
		for (JsonElement element : object.getAsJsonArray("choices")) {
			JsonObject choice = element.getAsJsonObject();
			choices.add(new ChoiceOption(
				choice.get("value").getAsString(),
				choice.has("label") ? choice.get("label").getAsString() : choice.get("value").getAsString()
			));
		}
		return choices;
	}

	private static ParameterType parseParameterType(String raw) {
		return switch (raw) {
			case "length" -> ParameterType.LENGTH;
			case "angle" -> ParameterType.ANGLE;
			case "count" -> ParameterType.COUNT;
			case "number" -> ParameterType.NUMBER;
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
			case NUMBER -> ParameterValue.number(element.getAsDouble());
			case ENUM -> ParameterValue.enumValue(element.getAsString());
			case BOOL -> ParameterValue.bool(element.getAsBoolean());
			case MATERIAL_REF -> ParameterValue.materialRef(element.getAsString());
		};
	}
}
