package dev.aperture.core.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.geometry.Facing;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.HostType;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parameter.ParameterDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JSON codec for {@link OpeningInstance}, aligned with {@code opening-instance.schema.json}.
 * Parameter types are resolved from the opening type definition on read.
 */
public final class OpeningInstanceCodec implements JsonCodec<OpeningInstance> {
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public OpeningInstance read(Path path, OpeningTypeDefinition definition) throws IOException {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return parse(JsonParser.parseReader(reader).getAsJsonObject(), definition);
		}
	}

	public OpeningInstance read(InputStream inputStream, OpeningTypeDefinition definition) throws IOException {
		try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			return parse(JsonParser.parseReader(reader).getAsJsonObject(), definition);
		}
	}

	@Override
	public String toJson(OpeningInstance instance) {
		JsonObject root = toJsonObject(instance);
		return gson.toJson(root);
	}

	@Override
	public OpeningInstance fromJson(String json, MigrationContext context) {
		throw new UnsupportedOperationException(
			"Use fromJson(String, OpeningTypeDefinition) or parse(JsonObject, OpeningTypeDefinition)"
		);
	}

	public OpeningInstance fromJson(String json, OpeningTypeDefinition definition) {
		return parse(JsonParser.parseString(json).getAsJsonObject(), definition);
	}

	public JsonObject toJsonObject(OpeningInstance instance) {
		JsonObject root = new JsonObject();
		root.addProperty("schemaVersion", instance.schemaVersion());
		root.addProperty("instanceId", instance.instanceId().toString());
		root.addProperty("typeId", instance.typeId().toString());
		root.add("parameters", writeParameters(instance.parameters()));
		root.add("transform", writeTransform(instance.transform()));
		root.add("host", writeHost(instance.host()));
		root.add("state", writeState(instance.state()));
		root.addProperty("revision", instance.revision());
		return root;
	}

	public OpeningInstance parse(JsonObject root, OpeningTypeDefinition definition) {
		int schemaVersion = root.get("schemaVersion").getAsInt();
		UUID instanceId = UUID.fromString(root.get("instanceId").getAsString());
		OpeningId typeId = OpeningId.parse(root.get("typeId").getAsString());

		if (!typeId.equals(definition.id())) {
			throw new IllegalArgumentException(
				"Type mismatch: instance has " + typeId + " but definition is " + definition.id()
			);
		}

		ParameterSet parameters = root.has("parameters")
			? readParameters(root.getAsJsonObject("parameters"), definition.parameters())
			: ParameterSet.empty();

		Transform3d transform = root.has("transform")
			? readTransform(root.getAsJsonObject("transform"))
			: Transform3d.at(0, 0, 0, Facing.NORTH);

		HostBinding host = root.has("host")
			? readHost(root.getAsJsonObject("host"))
			: HostBinding.freeStanding();

		OpeningState state = root.has("state")
			? readState(root.getAsJsonObject("state"))
			: OpeningState.CLOSED;

		long revision = root.has("revision") ? root.get("revision").getAsLong() : 0L;

		return OpeningInstance.builder(typeId)
			.instanceId(instanceId)
			.parameters(parameters)
			.transform(transform)
			.host(host)
			.state(state)
			.revision(revision)
			.build();
	}

	private static JsonObject writeParameters(ParameterSet parameters) {
		JsonObject object = new JsonObject();
		for (Map.Entry<String, ParameterValue> entry : parameters.asMap().entrySet()) {
			object.add(entry.getKey(), writeParameterValue(entry.getValue()));
		}
		return object;
	}

	private static JsonElement writeParameterValue(ParameterValue value) {
		return switch (value) {
			case ParameterValue.LengthValue length -> gsonNumber(length.millimeters());
			case ParameterValue.AngleValue angle -> gsonNumber(angle.degrees());
			case ParameterValue.CountValue count -> gsonNumber(count.value());
			case ParameterValue.EnumValue enumValue -> gsonString(enumValue.value());
			case ParameterValue.BoolValue bool -> gsonBool(bool.value());
			case ParameterValue.MaterialRefValue materialRef -> gsonString(materialRef.raw());
		};
	}

	private static com.google.gson.JsonPrimitive gsonNumber(double value) {
		return new com.google.gson.JsonPrimitive(value);
	}

	private static com.google.gson.JsonPrimitive gsonNumber(int value) {
		return new com.google.gson.JsonPrimitive(value);
	}

	private static com.google.gson.JsonPrimitive gsonString(String value) {
		return new com.google.gson.JsonPrimitive(value);
	}

	private static com.google.gson.JsonPrimitive gsonBool(boolean value) {
		return new com.google.gson.JsonPrimitive(value);
	}

	private static ParameterSet readParameters(
		JsonObject object,
		Map<String, ParameterDefinition> schema
	) {
		ParameterSet.Builder builder = ParameterSet.builder();
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			ParameterDefinition definition = schema.get(entry.getKey());
			if (definition == null) {
				throw new IllegalArgumentException("Unknown parameter in instance: " + entry.getKey());
			}
			builder.put(entry.getKey(), readParameterValue(definition.type(), entry.getValue()));
		}
		return builder.build();
	}

	private static ParameterValue readParameterValue(ParameterType type, JsonElement element) {
		return switch (type) {
			case LENGTH -> ParameterValue.length(element.getAsDouble());
			case ANGLE -> ParameterValue.angle(element.getAsDouble());
			case COUNT -> ParameterValue.count(element.getAsInt());
			case ENUM -> ParameterValue.enumValue(element.getAsString());
			case BOOL -> ParameterValue.bool(element.getAsBoolean());
			case MATERIAL_REF -> ParameterValue.materialRef(element.getAsString());
		};
	}

	private static JsonObject writeTransform(Transform3d transform) {
		JsonObject object = new JsonObject();
		object.addProperty("x", transform.origin().x());
		object.addProperty("y", transform.origin().y());
		object.addProperty("z", transform.origin().z());
		object.addProperty("facing", transform.facing().name().toLowerCase());
		return object;
	}

	private static Transform3d readTransform(JsonObject object) {
		double x = object.get("x").getAsDouble();
		double y = object.get("y").getAsDouble();
		double z = object.get("z").getAsDouble();
		Facing facing = Facing.fromId(object.get("facing").getAsString());
		return new Transform3d(new Vec3d(x, y, z), facing);
	}

	private static JsonObject writeHost(HostBinding host) {
		JsonObject object = new JsonObject();
		object.addProperty("type", host.type().id());
		if (!host.anchor().isEmpty()) {
			object.addProperty("anchor", host.anchor());
		}
		return object;
	}

	private static HostBinding readHost(JsonObject object) {
		HostType type = HostType.fromId(object.get("type").getAsString());
		String anchor = object.has("anchor") ? object.get("anchor").getAsString() : "";
		return new HostBinding(type, anchor);
	}

	private static JsonObject writeState(OpeningState state) {
		JsonObject object = new JsonObject();
		object.addProperty("openRatio", state.openRatio());
		return object;
	}

	private static OpeningState readState(JsonObject object) {
		double openRatio = object.has("openRatio") ? object.get("openRatio").getAsDouble() : 0.0;
		return new OpeningState(openRatio);
	}
}
