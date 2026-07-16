package dev.aperture.fabric.serialization;

import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.HostType;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NBT codec for {@link OpeningInstance} using Minecraft's {@link ValueInput}/{@link ValueOutput}.
 */
public final class OpeningInstanceNbtCodec {
	private OpeningInstanceNbtCodec() {
	}

	public static void write(ValueOutput output, OpeningInstance instance) {
		output.storeInt("schemaVersion", instance.schemaVersion());
		output.storeLong("instanceIdMSB", instance.instanceId().getMostSignificantBits());
		output.storeLong("instanceIdLSB", instance.instanceId().getLeastSignificantBits());
		output.storeString("typeId", instance.typeId().namespace() + ":" + instance.typeId().path());
		writeParameters(output, instance.parameters());
		writeTransform(output, instance.transform());
		writeHostBinding(output, instance.host());
		output.storeDouble("openRatio", instance.state().openRatio());
		output.storeLong("revision", instance.revision());
	}

	public static OpeningInstance read(ValueInput input) {
		int schemaVersion = input.readInt("schemaVersion").orElse(1);
		long msb = input.readLong("instanceIdMSB").orElse(0L);
		long lsb = input.readLong("instanceIdLSB").orElse(0L);
		UUID instanceId = new UUID(msb, lsb);
		OpeningId typeId = OpeningId.parse(input.readString("typeId").orElse("aperture:unknown"));
		ParameterSet parameters = readParameters(input);
		Transform3d transform = readTransform(input);
		HostBinding host = readHostBinding(input);
		OpeningState state = new OpeningState(input.readDouble("openRatio").orElse(0.0));
		long revision = input.readLong("revision").orElse(0L);
		return new OpeningInstance(schemaVersion, instanceId, typeId, parameters, transform, host, state, revision);
	}

	// --- Parameters ---

	private static void writeParameters(ValueOutput output, ParameterSet parameters) {
		int index = 0;
		for (Map.Entry<String, ParameterValue> entry : parameters.asMap().entrySet()) {
			String prefix = "param_" + index + "_";
			output.storeString(prefix + "key", entry.getKey());
			writeParameterValue(output, prefix, entry.getValue());
			index++;
		}
		output.storeInt("paramCount", index);
	}

	private static ParameterSet readParameters(ValueInput input) {
		int count = input.readInt("paramCount").orElse(0);
		Map<String, ParameterValue> values = new LinkedHashMap<>();
		for (int i = 0; i < count; i++) {
			String prefix = "param_" + i + "_";
			String key = input.readString(prefix + "key").orElse(null);
			ParameterValue value = readParameterValue(input, prefix);
			if (key != null && value != null) {
				values.put(key, value);
			}
		}
		return ParameterSet.of(values);
	}

	private static void writeParameterValue(ValueOutput output, String prefix, ParameterValue value) {
		output.storeString(prefix + "type", value.type().name());
		switch (value) {
			case ParameterValue.LengthValue v -> output.storeDouble(prefix + "val", v.millimeters());
			case ParameterValue.AngleValue v -> output.storeDouble(prefix + "val", v.degrees());
			case ParameterValue.CountValue v -> output.storeInt(prefix + "val_i", v.value());
			case ParameterValue.NumberValue v -> output.storeDouble(prefix + "val", v.value());
			case ParameterValue.BoolValue v -> output.storeBoolean(prefix + "val_b", v.value());
			case ParameterValue.EnumValue v -> output.storeString(prefix + "val_s", v.value());
			case ParameterValue.MaterialRefValue v -> output.storeString(prefix + "val_s", v.raw());
		}
	}

	private static ParameterValue readParameterValue(ValueInput input, String prefix) {
		String typeName = input.readString(prefix + "type").orElse(null);
		if (typeName == null) return null;
		ParameterType type;
		try {
			type = ParameterType.valueOf(typeName);
		} catch (IllegalArgumentException e) {
			return null;
		}
		return switch (type) {
			case LENGTH -> ParameterValue.length(input.readDouble(prefix + "val").orElse(0.0));
			case ANGLE -> ParameterValue.angle(input.readDouble(prefix + "val").orElse(0.0));
			case COUNT -> ParameterValue.count(input.readInt(prefix + "val_i").orElse(0));
			case NUMBER -> ParameterValue.number(input.readDouble(prefix + "val").orElse(0.0));
			case BOOL -> ParameterValue.bool(input.readBoolean(prefix + "val_b").orElse(false));
			case ENUM -> ParameterValue.enumValue(input.readString(prefix + "val_s").orElse(""));
			case MATERIAL_REF -> ParameterValue.materialRef(input.readString(prefix + "val_s").orElse(""));
		};
	}

	// --- Transform ---

	private static void writeTransform(ValueOutput output, Transform3d transform) {
		output.storeDouble("tx", transform.origin().x());
		output.storeDouble("ty", transform.origin().y());
		output.storeDouble("tz", transform.origin().z());
		output.storeString("facing", transform.facing().name());
		output.storeDouble("rotAxisOx", transform.rotationAxisOrigin().x());
		output.storeDouble("rotAxisOy", transform.rotationAxisOrigin().y());
		output.storeDouble("rotAxisOz", transform.rotationAxisOrigin().z());
		output.storeDouble("rotAxisDx", transform.rotationAxisDirection().x());
		output.storeDouble("rotAxisDy", transform.rotationAxisDirection().y());
		output.storeDouble("rotAxisDz", transform.rotationAxisDirection().z());
		output.storeDouble("rotRadians", transform.rotationRadians());
	}

	private static Transform3d readTransform(ValueInput input) {
		double tx = input.readDouble("tx").orElse(0.0);
		double ty = input.readDouble("ty").orElse(0.0);
		double tz = input.readDouble("tz").orElse(0.0);
		String facingName = input.readString("facing").orElse(Facing.NORTH.name());
		Facing facing;
		try {
			facing = Facing.valueOf(facingName);
		} catch (IllegalArgumentException e) {
			facing = Facing.NORTH;
		}
		double rotAxisOx = input.readDouble("rotAxisOx").orElse(0.0);
		double rotAxisOy = input.readDouble("rotAxisOy").orElse(0.0);
		double rotAxisOz = input.readDouble("rotAxisOz").orElse(0.0);
		double rotAxisDx = input.readDouble("rotAxisDx").orElse(0.0);
		double rotAxisDy = input.readDouble("rotAxisDy").orElse(1.0);
		double rotAxisDz = input.readDouble("rotAxisDz").orElse(0.0);
		double rotRadians = input.readDouble("rotRadians").orElse(0.0);
		return new Transform3d(
			new Vec3d(tx, ty, tz),
			facing,
			new Vec3d(rotAxisOx, rotAxisOy, rotAxisOz),
			new Vec3d(rotAxisDx, rotAxisDy, rotAxisDz),
			rotRadians
		);
	}

	// --- HostBinding ---

	private static void writeHostBinding(ValueOutput output, HostBinding host) {
		output.storeString("hostType", host.type().name());
		output.storeString("hostAnchor", host.anchor());
	}

	private static HostBinding readHostBinding(ValueInput input) {
		String typeName = input.readString("hostType").orElse(HostType.FREE_STANDING.name());
		String anchor = input.readString("hostAnchor").orElse("");
		HostType type;
		try {
			type = HostType.valueOf(typeName);
		} catch (IllegalArgumentException e) {
			type = HostType.FREE_STANDING;
		}
		return new HostBinding(type, anchor);
	}
}
