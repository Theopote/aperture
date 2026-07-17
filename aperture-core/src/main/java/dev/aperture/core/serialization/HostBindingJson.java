package dev.aperture.core.serialization;

import com.google.gson.JsonObject;
import dev.aperture.core.instance.HostAttachmentMode;
import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.HostFeatureId;
import dev.aperture.core.instance.HostFeatureType;
import dev.aperture.core.instance.HostType;
import dev.aperture.core.object.ArchitecturalObjectId;
import dev.aperture.math.LocalFrame;
import dev.aperture.math.Vec3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;

import java.util.Map;

/** JSON persistence for structured host bindings with legacy type/anchor migration. */
public final class HostBindingJson {
	private HostBindingJson() { }

	public static JsonObject write(HostBinding host) {
		JsonObject object = new JsonObject();
		object.addProperty("hostId", host.hostId().toString());
		JsonObject feature = new JsonObject();
		feature.addProperty("type", host.featureId().type().name().toLowerCase());
		feature.addProperty("id", host.featureId().value());
		object.add("feature", feature);
		object.add("insertionFrame", writeFrame(host.insertionFrame()));
		object.addProperty("mode", host.mode().name().toLowerCase());
		object.add("attachmentParameters", writeParameters(host.attachmentParameters()));
		object.addProperty("hostRevision", host.hostRevision());
		return object;
	}

	public static HostBinding read(JsonObject object) {
		if (!object.has("hostId")) {
			HostType type = HostType.fromId(object.get("type").getAsString());
			String anchor = object.has("anchor") ? object.get("anchor").getAsString() : "";
			return new HostBinding(type, anchor);
		}
		JsonObject feature = object.getAsJsonObject("feature");
		return new HostBinding(
			ArchitecturalObjectId.parse(object.get("hostId").getAsString()),
			new HostFeatureId(
				HostFeatureType.valueOf(feature.get("type").getAsString().toUpperCase()),
				feature.get("id").getAsString()
			),
			readFrame(object.getAsJsonObject("insertionFrame")),
			HostAttachmentMode.valueOf(object.get("mode").getAsString().toUpperCase()),
			object.has("attachmentParameters")
				? readParameters(object.getAsJsonObject("attachmentParameters"))
				: ParameterSet.empty(),
			object.has("hostRevision") ? object.get("hostRevision").getAsLong() : 0L
		);
	}

	private static JsonObject writeFrame(LocalFrame frame) {
		JsonObject object = new JsonObject();
		object.add("origin", writeVector(frame.origin()));
		object.add("xAxis", writeVector(frame.xAxis()));
		object.add("yAxis", writeVector(frame.yAxis()));
		object.add("zAxis", writeVector(frame.zAxis()));
		return object;
	}

	private static LocalFrame readFrame(JsonObject object) {
		return new LocalFrame(readVector(object.getAsJsonObject("origin")), readVector(object.getAsJsonObject("xAxis")),
			readVector(object.getAsJsonObject("yAxis")), readVector(object.getAsJsonObject("zAxis")));
	}

	private static JsonObject writeVector(Vec3d vector) {
		JsonObject object = new JsonObject();
		object.addProperty("x", vector.x());
		object.addProperty("y", vector.y());
		object.addProperty("z", vector.z());
		return object;
	}

	private static Vec3d readVector(JsonObject object) {
		return new Vec3d(object.get("x").getAsDouble(), object.get("y").getAsDouble(), object.get("z").getAsDouble());
	}

	private static JsonObject writeParameters(ParameterSet parameters) {
		JsonObject object = new JsonObject();
		for (Map.Entry<String, ParameterValue> entry : parameters.asMap().entrySet()) {
			JsonObject encoded = new JsonObject();
			encoded.addProperty("type", entry.getValue().type().name().toLowerCase());
			switch (entry.getValue()) {
				case ParameterValue.LengthValue value -> encoded.addProperty("value", value.millimeters());
				case ParameterValue.AngleValue value -> encoded.addProperty("value", value.degrees());
				case ParameterValue.CountValue value -> encoded.addProperty("value", value.value());
				case ParameterValue.NumberValue value -> encoded.addProperty("value", value.value());
				case ParameterValue.BoolValue value -> encoded.addProperty("value", value.value());
				case ParameterValue.EnumValue value -> encoded.addProperty("value", value.value());
				case ParameterValue.MaterialRefValue value -> encoded.addProperty("value", value.raw());
			}
			object.add(entry.getKey(), encoded);
		}
		return object;
	}

	private static ParameterSet readParameters(JsonObject object) {
		ParameterSet.Builder builder = ParameterSet.builder();
		object.entrySet().forEach(entry -> {
			JsonObject encoded = entry.getValue().getAsJsonObject();
			ParameterType type = ParameterType.valueOf(encoded.get("type").getAsString().toUpperCase());
			builder.put(entry.getKey(), switch (type) {
				case LENGTH -> ParameterValue.length(encoded.get("value").getAsDouble());
				case ANGLE -> ParameterValue.angle(encoded.get("value").getAsDouble());
				case COUNT -> ParameterValue.count(encoded.get("value").getAsInt());
				case NUMBER -> ParameterValue.number(encoded.get("value").getAsDouble());
				case BOOL -> ParameterValue.bool(encoded.get("value").getAsBoolean());
				case ENUM -> ParameterValue.enumValue(encoded.get("value").getAsString());
				case MATERIAL_REF -> ParameterValue.materialRef(encoded.get("value").getAsString());
			});
		});
		return builder.build();
	}
}
