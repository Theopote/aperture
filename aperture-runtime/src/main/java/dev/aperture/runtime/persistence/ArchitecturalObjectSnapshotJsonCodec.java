package dev.aperture.runtime.persistence;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.behavior.BehaviorId;
import dev.aperture.runtime.model.object.ArchitecturalFamilyId;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.object.ArchitecturalObjectInstance;
import dev.aperture.runtime.model.object.ArchitecturalTypeId;
import dev.aperture.runtime.model.object.HostBinding;
import dev.aperture.runtime.model.persistence.ArchitecturalObjectSnapshot;
import dev.aperture.runtime.model.persistence.BehaviorConfiguration;
import dev.aperture.runtime.model.state.StateRevision;
import dev.aperture.runtime.model.state.StateValue;
import dev.aperture.runtime.model.state.StateValueType;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Canonical JSON wire format shared by NBT adapters, tests, and future external stores. */
public final class ArchitecturalObjectSnapshotJsonCodec {
	private static final Gson GSON = new Gson();
	private static final Type STRING_MAP = new TypeToken<Map<String, String>>() { }.getType();
	private static final Type OBJECT_MAP = new TypeToken<Map<String, Object>>() { }.getType();

	public String encode(ArchitecturalObjectSnapshot snapshot) {
		JsonObject root = new JsonObject();
		root.addProperty("snapshotSchemaVersion", snapshot.schemaVersion());
		writeInstance(root, snapshot.instance());
		root.add("persistentState", writeState(snapshot.persistentState()));
		root.addProperty("stateRevision", snapshot.stateRevision().value());
		root.addProperty("stateTimestamp", snapshot.stateTimestamp().toString());
		JsonArray behaviors = new JsonArray();
		for (BehaviorConfiguration behavior : snapshot.behaviors()) {
			JsonObject json = new JsonObject();
			json.addProperty("id", behavior.behaviorId().value());
			json.addProperty("version", behavior.version());
			json.add("values", GSON.toJsonTree(behavior.values()));
			behaviors.add(json);
		}
		root.add("behaviors", behaviors);
		return GSON.toJson(root);
	}

	public ArchitecturalObjectSnapshot decode(String json) {
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		ArchitecturalObjectInstance instance = readInstance(root);
		Map<String, StateValue> state = readState(root.getAsJsonObject("persistentState"));
		List<BehaviorConfiguration> behaviors = new ArrayList<>();
		for (JsonElement element : root.getAsJsonArray("behaviors")) {
			JsonObject value = element.getAsJsonObject();
			behaviors.add(new BehaviorConfiguration(new BehaviorId(value.get("id").getAsString()),
				value.get("version").getAsInt(), GSON.fromJson(value.get("values"), OBJECT_MAP)));
		}
		return new ArchitecturalObjectSnapshot(root.get("snapshotSchemaVersion").getAsInt(), instance, state,
			new StateRevision(root.get("stateRevision").getAsLong()),
			Instant.parse(root.get("stateTimestamp").getAsString()), behaviors);
	}

	private static void writeInstance(JsonObject root, ArchitecturalObjectInstance instance) {
		root.addProperty("instanceSchemaVersion", instance.schemaVersion());
		root.addProperty("objectId", instance.objectId().toString());
		root.addProperty("typeId", instance.typeId().toString());
		root.addProperty("familyId", instance.familyId().value());
		root.add("parameters", writeParameters(instance.parameterOverrides()));
		root.add("transform", writeTransform(instance.transform()));
		JsonArray hosts = new JsonArray();
		for (HostBinding host : instance.hostBindings()) {
			JsonObject value = new JsonObject();
			value.addProperty("hostId", host.hostId().toString());
			value.addProperty("featureId", host.featureId());
			value.add("insertionFrame", writeTransform(host.insertionFrame()));
			value.addProperty("attachmentMode", host.attachmentMode());
			value.add("attachmentParameters", writeParameters(host.attachmentParameters()));
			value.addProperty("hostRevision", host.hostRevision());
			hosts.add(value);
		}
		root.add("hostBindings", hosts);
		root.addProperty("objectRevision", instance.revision());
		root.add("metadata", GSON.toJsonTree(instance.metadata()));
	}

	private static ArchitecturalObjectInstance readInstance(JsonObject root) {
		List<HostBinding> hosts = new ArrayList<>();
		for (JsonElement element : root.getAsJsonArray("hostBindings")) {
			JsonObject host = element.getAsJsonObject();
			hosts.add(new HostBinding(ArchitecturalObjectId.parse(host.get("hostId").getAsString()),
				host.get("featureId").getAsString(), readTransform(host.getAsJsonObject("insertionFrame")),
				host.get("attachmentMode").getAsString(), readParameters(host.getAsJsonObject("attachmentParameters")),
				host.get("hostRevision").getAsLong()));
		}
		Map<String, StateValue> persistent = readState(root.getAsJsonObject("persistentState"));
		Map<String, Object> instanceState = new LinkedHashMap<>();
		instanceState.putAll(persistent);
		return new ArchitecturalObjectInstance(root.get("instanceSchemaVersion").getAsInt(),
			ArchitecturalObjectId.parse(root.get("objectId").getAsString()),
			ArchitecturalTypeId.parse(root.get("typeId").getAsString()),
			new ArchitecturalFamilyId(root.get("familyId").getAsString()),
			readParameters(root.getAsJsonObject("parameters")), readTransform(root.getAsJsonObject("transform")),
			hosts, instanceState, root.get("objectRevision").getAsLong(), GSON.fromJson(root.get("metadata"), STRING_MAP));
	}

	private static JsonObject writeState(Map<String, StateValue> state) {
		JsonObject result = new JsonObject();
		state.forEach((name, value) -> {
			JsonObject json = new JsonObject();
			json.addProperty("type", value.type().name());
			switch (value) {
				case StateValue.BooleanValue v -> json.addProperty("value", v.value());
				case StateValue.NumberValue v -> json.addProperty("value", v.value());
				case StateValue.EnumValue v -> json.addProperty("value", v.value());
				case StateValue.StringValue v -> json.addProperty("value", v.value());
				case StateValue.ReferenceValue v -> json.addProperty("value", v.value());
				case StateValue.VectorValue v -> json.add("value", writeVector(v.value()));
				case StateValue.TransformValue v -> json.add("value", writeTransform(v.value()));
				case StateValue.TimestampValue v -> json.addProperty("value", v.value().toString());
			}
			result.add(name, json);
		});
		return result;
	}

	private static Map<String, StateValue> readState(JsonObject object) {
		Map<String, StateValue> result = new LinkedHashMap<>();
		object.entrySet().forEach(entry -> {
			JsonObject value = entry.getValue().getAsJsonObject();
			StateValueType type = StateValueType.valueOf(value.get("type").getAsString());
			JsonElement raw = value.get("value");
			StateValue decoded = switch (type) {
				case BOOLEAN -> StateValue.bool(raw.getAsBoolean());
				case NUMBER -> StateValue.number(raw.getAsDouble());
				case ENUM -> StateValue.enumeration(raw.getAsString());
				case STRING -> StateValue.string(raw.getAsString());
				case REFERENCE -> StateValue.reference(raw.getAsString());
				case VECTOR -> StateValue.vector(readVector(raw.getAsJsonObject()));
				case TRANSFORM -> StateValue.transform(readTransform(raw.getAsJsonObject()));
				case TIMESTAMP -> StateValue.timestamp(Instant.parse(raw.getAsString()));
			};
			result.put(entry.getKey(), decoded);
		});
		return Map.copyOf(result);
	}

	private static JsonObject writeParameters(ParameterSet parameters) {
		JsonObject result = new JsonObject();
		parameters.asMap().forEach((name, value) -> {
			JsonObject json = new JsonObject(); json.addProperty("type", value.type().name());
			switch (value) {
				case ParameterValue.LengthValue v -> json.addProperty("value", v.millimeters());
				case ParameterValue.AngleValue v -> json.addProperty("value", v.degrees());
				case ParameterValue.CountValue v -> json.addProperty("value", v.value());
				case ParameterValue.NumberValue v -> json.addProperty("value", v.value());
				case ParameterValue.BoolValue v -> json.addProperty("value", v.value());
				case ParameterValue.EnumValue v -> json.addProperty("value", v.value());
				case ParameterValue.MaterialRefValue v -> json.addProperty("value", v.raw());
			}
			result.add(name, json);
		});
		return result;
	}

	private static ParameterSet readParameters(JsonObject object) {
		ParameterSet.Builder result = ParameterSet.builder();
		object.entrySet().forEach(entry -> {
			JsonObject value = entry.getValue().getAsJsonObject(); JsonElement raw = value.get("value");
			ParameterValue decoded = switch (ParameterType.valueOf(value.get("type").getAsString())) {
				case LENGTH -> ParameterValue.length(raw.getAsDouble()); case ANGLE -> ParameterValue.angle(raw.getAsDouble());
				case COUNT -> ParameterValue.count(raw.getAsInt()); case NUMBER -> ParameterValue.number(raw.getAsDouble());
				case BOOL -> ParameterValue.bool(raw.getAsBoolean()); case ENUM -> ParameterValue.enumValue(raw.getAsString());
				case MATERIAL_REF -> ParameterValue.materialRef(raw.getAsString());
			}; result.put(entry.getKey(), decoded);
		});
		return result.build();
	}

	private static JsonObject writeTransform(Transform3d value) {
		JsonObject json = new JsonObject(); json.add("origin", writeVector(value.origin())); json.addProperty("facing", value.facing().name());
		json.add("axisOrigin", writeVector(value.rotationAxisOrigin())); json.add("axisDirection", writeVector(value.rotationAxisDirection()));
		json.addProperty("radians", value.rotationRadians()); return json;
	}
	private static Transform3d readTransform(JsonObject value) {
		return new Transform3d(readVector(value.getAsJsonObject("origin")), Facing.valueOf(value.get("facing").getAsString()),
			readVector(value.getAsJsonObject("axisOrigin")), readVector(value.getAsJsonObject("axisDirection")), value.get("radians").getAsDouble());
	}
	private static JsonObject writeVector(Vec3d value) {
		JsonObject json = new JsonObject(); json.addProperty("x", value.x()); json.addProperty("y", value.y()); json.addProperty("z", value.z()); return json;
	}
	private static Vec3d readVector(JsonObject value) { return new Vec3d(value.get("x").getAsDouble(), value.get("y").getAsDouble(), value.get("z").getAsDouble()); }
}
