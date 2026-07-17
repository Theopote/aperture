package dev.aperture.core.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.core.instance.OpeningStateSchemas;
import dev.aperture.core.state.RuntimeState;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parametric.ParameterSetJson;
import dev.aperture.parameter.ParameterSet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * JSON codec for {@link OpeningInstance}, aligned with {@code opening-instance.schema.json}.
 * The {@code parameters} object stores sparse overrides only; defaults come from the opening type.
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
		root.add("parameters", ParameterSetJson.writeOverrides(instance.parameters()));
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
			? ParameterSetJson.readOverrides(definition.parametricSchema(), root.getAsJsonObject("parameters"))
			: ParameterSet.empty();

		Transform3d transform = root.has("transform")
			? readTransform(root.getAsJsonObject("transform"))
			: Transform3d.at(0, 0, 0, Facing.NORTH);

		HostBinding host = root.has("host")
			? readHost(root.getAsJsonObject("host"))
			: HostBinding.freeStanding();

		var stateSchema = definition.stateSchema().properties().isEmpty()
			? OpeningStateSchemas.OPERABLE
			: definition.stateSchema();
		OpeningState state = root.has("state")
			? readState(root.getAsJsonObject("state"), stateSchema)
			: new OpeningState(RuntimeState.initial(stateSchema));

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
		return HostBindingJson.write(host);
	}

	private static HostBinding readHost(JsonObject object) {
		return HostBindingJson.read(object);
	}

	private static JsonObject writeState(OpeningState state) {
		return RuntimeStateJson.writePersistent(state.runtimeState());
	}

	private static OpeningState readState(JsonObject object, dev.aperture.core.state.StateSchema schema) {
		return new OpeningState(RuntimeStateJson.readPersistent(schema, object));
	}
}
