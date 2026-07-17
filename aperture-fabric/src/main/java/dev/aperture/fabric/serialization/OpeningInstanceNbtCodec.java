package dev.aperture.fabric.serialization;

import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.HostType;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.core.instance.OpeningStateSchemas;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.state.RuntimeState;
import dev.aperture.core.state.StatePropertyType;
import dev.aperture.core.state.StatePropertyDefinition;
import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.time.Instant;
import java.time.format.DateTimeParseException;
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
output.putInt("schemaVersion", instance.schemaVersion());
output.putLong("instanceIdMSB", instance.instanceId().getMostSignificantBits());
output.putLong("instanceIdLSB", instance.instanceId().getLeastSignificantBits());
output.putString("typeId", instance.typeId().namespace() + ":" + instance.typeId().path());
writeParameters(output, instance.parameters());
writeTransform(output, instance.transform());
writeHostBinding(output, instance.host());
writeRuntimeState(output, instance.state().runtimeState());
output.putLong("revision", instance.revision());
}

public static OpeningInstance read(ValueInput input) {
int schemaVersion = input.getIntOr("schemaVersion", 1);
long msb = input.getLongOr("instanceIdMSB", 0L);
long lsb = input.getLongOr("instanceIdLSB", 0L);
UUID instanceId = msb == 0L && lsb == 0L ? UUID.randomUUID() : new UUID(msb, lsb);
OpeningId typeId = OpeningId.parse(input.getStringOr("typeId", "aperture:unknown"));
ParameterSet parameters = readParameters(input);
Transform3d transform = readTransform(input);
HostBinding host = readHostBinding(input);
OpeningState state = readRuntimeState(input);
long revision = input.getLongOr("revision", 0L);
return new OpeningInstance(schemaVersion, instanceId, typeId, parameters, transform, host, state, revision);
}

// --- Runtime state ---

private static void writeRuntimeState(ValueOutput output, RuntimeState state) {
output.putLong("stateRevision", state.revision());
output.putString("stateTimestamp", state.timestamp().toString());
int index = 0;
for (Map.Entry<String, Object> entry : state.persistentProperties().entrySet()) {
String prefix = "state_" + index + "_";
StatePropertyType type = state.schema().require(entry.getKey()).type();
output.putString(prefix + "key", entry.getKey());
output.putString(prefix + "type", type.name());
switch (type) {
case NUMBER -> output.putDouble(prefix + "number", ((Number) entry.getValue()).doubleValue());
case BOOLEAN -> output.putBoolean(prefix + "boolean", (Boolean) entry.getValue());
case STRING, ENUM -> output.putString(prefix + "string", (String) entry.getValue());
}
index++;
}
output.putInt("statePropertyCount", index);
}

private static OpeningState readRuntimeState(ValueInput input) {
int count = input.getIntOr("statePropertyCount", 0);
if (count == 0) {
return new OpeningState(input.getDoubleOr("openRatio", 0.0));
}

Map<String, Object> persistent = new LinkedHashMap<>();
for (int i = 0; i < count; i++) {
String prefix = "state_" + i + "_";
String key = input.getString(prefix + "key").orElse(null);
StatePropertyType type = parseStatePropertyType(input.getString(prefix + "type").orElse(null));
StatePropertyDefinition definition = key == null ? null : OpeningStateSchemas.OPERABLE.property(key).orElse(null);
if (definition == null || !definition.persistent() || definition.type() != type) continue;
Object value = switch (type) {
case NUMBER -> input.getDoubleOr(prefix + "number", 0.0);
case BOOLEAN -> input.getBooleanOr(prefix + "boolean", false);
case STRING, ENUM -> input.getStringOr(prefix + "string", "");
};
persistent.put(key, value);
}

long revision = Math.max(0L, input.getLongOr("stateRevision", 0L));
Instant timestamp = parseTimestamp(input.getStringOr("stateTimestamp", Instant.EPOCH.toString()));
return new OpeningState(RuntimeState.restore(OpeningStateSchemas.OPERABLE, persistent, revision, timestamp));
}

private static StatePropertyType parseStatePropertyType(String value) {
if (value == null) return null;
try {
return StatePropertyType.valueOf(value);
} catch (IllegalArgumentException ignored) {
return null;
}
}

private static Instant parseTimestamp(String value) {
try {
return Instant.parse(value);
} catch (DateTimeParseException ignored) {
return Instant.EPOCH;
}
}

// --- Parameters ---

private static void writeParameters(ValueOutput output, ParameterSet parameters) {
int index = 0;
for (Map.Entry<String, ParameterValue> entry : parameters.asMap().entrySet()) {
String prefix = "param_" + index + "_";
output.putString(prefix + "key", entry.getKey());
writeParameterValue(output, prefix, entry.getValue());
index++;
}
output.putInt("paramCount", index);
}

private static ParameterSet readParameters(ValueInput input) {
int count = input.getIntOr("paramCount", 0);
Map<String, ParameterValue> values = new LinkedHashMap<>();
for (int i = 0; i < count; i++) {
String prefix = "param_" + i + "_";
String key = input.getString(prefix + "key").orElse(null);
ParameterValue value = readParameterValue(input, prefix);
if (key != null && value != null) {
values.put(key, value);
}
}
return ParameterSet.of(values);
}

private static void writeParameterValue(ValueOutput output, String prefix, ParameterValue value) {
output.putString(prefix + "type", value.type().name());
switch (value) {
case ParameterValue.LengthValue v -> output.putDouble(prefix + "val", v.millimeters());
case ParameterValue.AngleValue v -> output.putDouble(prefix + "val", v.degrees());
case ParameterValue.CountValue v -> output.putInt(prefix + "val_i", v.value());
case ParameterValue.NumberValue v -> output.putDouble(prefix + "val", v.value());
case ParameterValue.BoolValue v -> output.putBoolean(prefix + "val_b", v.value());
case ParameterValue.EnumValue v -> output.putString(prefix + "val_s", v.value());
case ParameterValue.MaterialRefValue v -> output.putString(prefix + "val_s", v.raw());
}
}

private static ParameterValue readParameterValue(ValueInput input, String prefix) {
String typeName = input.getString(prefix + "type").orElse(null);
if (typeName == null) return null;
ParameterType type;
try {
type = ParameterType.valueOf(typeName);
} catch (IllegalArgumentException e) {
return null;
}
return switch (type) {
case LENGTH -> ParameterValue.length(input.getDoubleOr(prefix + "val", 0.0));
case ANGLE -> ParameterValue.angle(input.getDoubleOr(prefix + "val", 0.0));
case COUNT -> ParameterValue.count(input.getIntOr(prefix + "val_i", 0));
case NUMBER -> ParameterValue.number(input.getDoubleOr(prefix + "val", 0.0));
case BOOL -> ParameterValue.bool(input.getBooleanOr(prefix + "val_b", false));
case ENUM -> ParameterValue.enumValue(input.getStringOr(prefix + "val_s", ""));
case MATERIAL_REF -> ParameterValue.materialRef(input.getStringOr(prefix + "val_s", ""));
};
}

// --- Transform ---

private static void writeTransform(ValueOutput output, Transform3d transform) {
output.putDouble("tx", transform.origin().x());
output.putDouble("ty", transform.origin().y());
output.putDouble("tz", transform.origin().z());
output.putString("facing", transform.facing().name());
output.putDouble("rotAxisOx", transform.rotationAxisOrigin().x());
output.putDouble("rotAxisOy", transform.rotationAxisOrigin().y());
output.putDouble("rotAxisOz", transform.rotationAxisOrigin().z());
output.putDouble("rotAxisDx", transform.rotationAxisDirection().x());
output.putDouble("rotAxisDy", transform.rotationAxisDirection().y());
output.putDouble("rotAxisDz", transform.rotationAxisDirection().z());
output.putDouble("rotRadians", transform.rotationRadians());
}

private static Transform3d readTransform(ValueInput input) {
double tx = input.getDoubleOr("tx", 0.0);
double ty = input.getDoubleOr("ty", 0.0);
double tz = input.getDoubleOr("tz", 0.0);
Facing facing;
try {
facing = Facing.valueOf(input.getStringOr("facing", Facing.NORTH.name()));
} catch (IllegalArgumentException e) {
facing = Facing.NORTH;
}
double rotAxisOx = input.getDoubleOr("rotAxisOx", 0.0);
double rotAxisOy = input.getDoubleOr("rotAxisOy", 0.0);
double rotAxisOz = input.getDoubleOr("rotAxisOz", 0.0);
double rotAxisDx = input.getDoubleOr("rotAxisDx", 0.0);
double rotAxisDy = input.getDoubleOr("rotAxisDy", 1.0);
double rotAxisDz = input.getDoubleOr("rotAxisDz", 0.0);
double rotRadians = input.getDoubleOr("rotRadians", 0.0);
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
output.putString("hostType", host.type().name());
output.putString("hostAnchor", host.anchor());
}

private static HostBinding readHostBinding(ValueInput input) {
HostType type;
try {
type = HostType.valueOf(input.getStringOr("hostType", HostType.FREE_STANDING.name()));
} catch (IllegalArgumentException e) {
type = HostType.FREE_STANDING;
}
String anchor = input.getStringOr("hostAnchor", "");
return new HostBinding(type, anchor);
}
}