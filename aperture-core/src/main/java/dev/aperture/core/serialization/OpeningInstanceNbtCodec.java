package dev.aperture.core.serialization;

import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.HostType;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * NBT codec for {@link OpeningInstance}.
 * Serializes to Minecraft NBT format for world persistence.
 */
public final class OpeningInstanceNbtCodec {

    /**
     * Write OpeningInstance to NBT.
     */
    public static void write(ValueOutput output, OpeningInstance instance) {
        output.storeInt("schemaVersion", instance.schemaVersion());
        output.store("instanceId", UUIDUtil.CODEC, instance.instanceId());
        output.storeString("typeId", instance.typeId().toString());

        // Write parameters
        writeParameters(output, instance.parameters());

        // Write transform
        writeTransform(output, instance.transform());

        // Write host binding
        writeHost(output, instance.host());

        // Write state
        writeState(output, instance.state());

        output.storeLong("revision", instance.revision());
    }

    /**
     * Read OpeningInstance from NBT.
     */
    public static OpeningInstance read(ValueInput input) {
        int schemaVersion = input.readInt("schemaVersion").orElse(1);
        UUID instanceId = input.read("instanceId", UUIDUtil.CODEC)
            .orElseThrow(() -> new IllegalStateException("Missing instanceId"));
        String typeIdStr = input.readString("typeId")
            .orElseThrow(() -> new IllegalStateException("Missing typeId"));
        OpeningId typeId = OpeningId.parse(typeIdStr);

        ParameterSet parameters = readParameters(input);
        Transform3d transform = readTransform(input);
        HostBinding host = readHost(input);
        OpeningState state = readState(input);
        long revision = input.readLong("revision").orElse(0L);

        return OpeningInstance.builder(typeId)
            .instanceId(instanceId)
            .parameters(parameters)
            .transform(transform)
            .host(host)
            .state(state)
            .revision(revision)
            .build();
    }

    private static void writeParameters(ValueOutput output, ParameterSet parameters) {
        Map<String, ParameterValue> values = parameters.asMap();
        output.storeInt("paramCount", values.size());

        int index = 0;
        for (Map.Entry<String, ParameterValue> entry : values.entrySet()) {
            String prefix = "param_" + index + "_";
            output.storeString(prefix + "key", entry.getKey());
            writeParameterValue(output, prefix, entry.getValue());
            index++;
        }
    }

    private static ParameterSet readParameters(ValueInput input) {
        int count = input.readInt("paramCount").orElse(0);
        ParameterSet.Builder builder = ParameterSet.builder();

        for (int i = 0; i < count; i++) {
            String prefix = "param_" + i + "_";
            String key = input.readString(prefix + "key").orElse("");
            if (!key.isEmpty()) {
                ParameterValue value = readParameterValue(input, prefix);
                if (value != null) {
                    builder.put(key, value);
                }
            }
        }

        return builder.build();
    }

    private static void writeParameterValue(ValueOutput output, String prefix, ParameterValue value) {
        output.storeString(prefix + "type", value.type().name());

        switch (value.type()) {
            case LENGTH -> {
                ParameterValue.LengthValue lv = (ParameterValue.LengthValue) value;
                output.storeDouble(prefix + "value", lv.millimeters());
            }
            case COUNT -> {
                ParameterValue.CountValue cv = (ParameterValue.CountValue) value;
                output.storeInt(prefix + "value", cv.value());
            }
            case ANGLE -> {
                ParameterValue.AngleValue av = (ParameterValue.AngleValue) value;
                output.storeDouble(prefix + "value", av.degrees());
            }
            case NUMBER -> {
                ParameterValue.NumberValue nv = (ParameterValue.NumberValue) value;
                output.storeDouble(prefix + "value", nv.value());
            }
            case BOOL -> {
                ParameterValue.BoolValue bv = (ParameterValue.BoolValue) value;
                output.storeBoolean(prefix + "value", bv.value());
            }
            case ENUM -> {
                ParameterValue.EnumValue ev = (ParameterValue.EnumValue) value;
                output.storeString(prefix + "value", ev.value());
            }
            case MATERIAL_REF -> {
                ParameterValue.MaterialRefValue mv = (ParameterValue.MaterialRefValue) value;
                output.storeString(prefix + "value", mv.raw());
            }
            case PROFILE_REF -> {
                ParameterValue.ProfileRefValue pv = (ParameterValue.ProfileRefValue) value;
                output.storeString(prefix + "value", pv.raw());
            }
        }
    }

    private static ParameterValue readParameterValue(ValueInput input, String prefix) {
        String typeStr = input.readString(prefix + "type").orElse("");
        if (typeStr.isEmpty()) {
            return null;
        }

        try {
            return switch (typeStr) {
                case "LENGTH" -> {
                    double mm = input.readDouble(prefix + "value").orElse(0.0);
                    yield ParameterValue.length(mm);
                }
                case "COUNT" -> {
                    int count = input.readInt(prefix + "value").orElse(0);
                    yield ParameterValue.count(count);
                }
                case "ANGLE" -> {
                    double degrees = input.readDouble(prefix + "value").orElse(0.0);
                    yield ParameterValue.angle(degrees);
                }
                case "NUMBER" -> {
                    double num = input.readDouble(prefix + "value").orElse(0.0);
                    yield ParameterValue.number(num);
                }
                case "BOOL" -> {
                    boolean bool = input.readBoolean(prefix + "value").orElse(false);
                    yield ParameterValue.bool(bool);
                }
                case "ENUM" -> {
                    String enumVal = input.readString(prefix + "value").orElse("");
                    yield ParameterValue.enumValue(enumVal);
                }
                case "MATERIAL_REF" -> {
                    String matRef = input.readString(prefix + "value").orElse("");
                    yield ParameterValue.materialRef(matRef);
                }
                case "PROFILE_REF" -> {
                    String profRef = input.readString(prefix + "value").orElse("");
                    yield ParameterValue.profileRef(profRef);
                }
                default -> null;
            };
        } catch (Exception e) {
            System.err.println("Failed to read parameter value: " + e.getMessage());
            return null;
        }
    }

    private static void writeTransform(ValueOutput output, Transform3d transform) {
        output.storeDouble("transform_x", transform.origin().x());
        output.storeDouble("transform_y", transform.origin().y());
        output.storeDouble("transform_z", transform.origin().z());
        output.storeString("transform_facing", transform.facing().name().toLowerCase());
    }

    private static Transform3d readTransform(ValueInput input) {
        double x = input.readDouble("transform_x").orElse(0.0);
        double y = input.readDouble("transform_y").orElse(0.0);
        double z = input.readDouble("transform_z").orElse(0.0);
        String facingStr = input.readString("transform_facing").orElse("north");
        Facing facing = Facing.fromId(facingStr);
        return new Transform3d(new Vec3d(x, y, z), facing);
    }

    private static void writeHost(ValueOutput output, HostBinding host) {
        output.storeString("host_type", host.type().id());
        if (!host.anchor().isEmpty()) {
            output.storeString("host_anchor", host.anchor());
        }
    }

    private static HostBinding readHost(ValueInput input) {
        String typeStr = input.readString("host_type").orElse("free_standing");
        HostType type = HostType.fromId(typeStr);
        String anchor = input.readString("host_anchor").orElse("");
        return new HostBinding(type, anchor);
    }

    private static void writeState(ValueOutput output, OpeningState state) {
        output.storeDouble("state_openRatio", state.openRatio());
    }

    private static OpeningState readState(ValueInput input) {
        double openRatio = input.readDouble("state_openRatio").orElse(0.0);
        return new OpeningState(openRatio);
    }
}
