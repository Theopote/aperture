package dev.aperture.core.serialization;

import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OpeningInstanceNbtCodec}.
 */
class OpeningInstanceNbtCodecTest {

    @Test
    void roundTrip_simpleInstance_preservesAllFields() {
        // Given: A simple opening instance
        UUID instanceId = UUID.randomUUID();
        OpeningId typeId = OpeningId.parse("aperture:fixed_window");

        ParameterSet parameters = ParameterSet.builder()
            .put("width", ParameterValue.length(1200.0))
            .put("height", ParameterValue.length(1500.0))
            .build();

        Transform3d transform = new Transform3d(new Vec3d(100, 64, 200), Facing.NORTH);

        OpeningInstance original = OpeningInstance.builder(typeId)
            .instanceId(instanceId)
            .parameters(parameters)
            .transform(transform)
            .host(HostBinding.freeStanding())
            .state(OpeningState.CLOSED)
            .revision(1L)
            .build();

        // When: Write to NBT and read back
        var nbt = new net.minecraft.nbt.CompoundTag();
        var output = ValueOutput.create(nbt, NbtOps.INSTANCE);
        OpeningInstanceNbtCodec.write(output, original);

        var input = ValueInput.create(nbt, NbtOps.INSTANCE);
        OpeningInstance restored = OpeningInstanceNbtCodec.read(input);

        // Then: All fields are preserved
        assertEquals(original.schemaVersion(), restored.schemaVersion());
        assertEquals(original.instanceId(), restored.instanceId());
        assertEquals(original.typeId(), restored.typeId());
        assertEquals(original.revision(), restored.revision());

        // Transform
        assertEquals(original.transform().origin().x(), restored.transform().origin().x(), 0.001);
        assertEquals(original.transform().origin().y(), restored.transform().origin().y(), 0.001);
        assertEquals(original.transform().origin().z(), restored.transform().origin().z(), 0.001);
        assertEquals(original.transform().facing(), restored.transform().facing());

        // Parameters
        assertEquals(1200.0, restored.parameters().requireLength("width"), 0.001);
        assertEquals(1500.0, restored.parameters().requireLength("height"), 0.001);

        // Host
        assertEquals(original.host().type(), restored.host().type());

        // State
        assertEquals(original.state().openRatio(), restored.state().openRatio(), 0.001);
    }

    @Test
    void roundTrip_allParameterTypes_preservesTypes() {
        // Given: Instance with all parameter types
        OpeningId typeId = OpeningId.parse("aperture:test_opening");

        ParameterSet parameters = ParameterSet.builder()
            .put("length_param", ParameterValue.length(1000.0))
            .put("count_param", ParameterValue.count(5))
            .put("angle_param", ParameterValue.angle(45.0))
            .put("number_param", ParameterValue.number(3.14))
            .put("bool_param", ParameterValue.bool(true))
            .put("enum_param", ParameterValue.enumValue("option_a"))
            .put("material_param", ParameterValue.materialRef("aperture:oak"))
            .put("profile_param", ParameterValue.profileRef("aperture:frame_l_50"))
            .build();

        OpeningInstance original = OpeningInstance.builder(typeId)
            .parameters(parameters)
            .build();

        // When: Round-trip through NBT
        var nbt = new net.minecraft.nbt.CompoundTag();
        var output = ValueOutput.create(nbt, NbtOps.INSTANCE);
        OpeningInstanceNbtCodec.write(output, original);

        var input = ValueInput.create(nbt, NbtOps.INSTANCE);
        OpeningInstance restored = OpeningInstanceNbtCodec.read(input);

        // Then: All parameter types preserved
        assertEquals(1000.0, restored.parameters().requireLength("length_param"), 0.001);
        assertEquals(5, restored.parameters().requireCount("count_param"));
        assertEquals(45.0, restored.parameters().requireAngle("angle_param"), 0.001);
        assertEquals(3.14, restored.parameters().requireNumber("number_param"), 0.001);
        assertTrue(restored.parameters().requireBool("bool_param"));
        assertEquals("option_a", restored.parameters().requireEnum("enum_param"));
        assertEquals("aperture:oak", restored.parameters().requireMaterialRef("material_param"));
    }

    @Test
    void roundTrip_emptyParameters_works() {
        // Given: Instance with no parameters
        OpeningInstance original = OpeningInstance.builder(OpeningId.parse("aperture:test"))
            .parameters(ParameterSet.empty())
            .build();

        // When: Round-trip through NBT
        var nbt = new net.minecraft.nbt.CompoundTag();
        var output = ValueOutput.create(nbt, NbtOps.INSTANCE);
        OpeningInstanceNbtCodec.write(output, original);

        var input = ValueInput.create(nbt, NbtOps.INSTANCE);
        OpeningInstance restored = OpeningInstanceNbtCodec.read(input);

        // Then: Empty parameters preserved
        assertEquals(0, restored.parameters().asMap().size());
    }

    @Test
    void roundTrip_differentFacings_preservesFacing() {
        for (Facing facing : Facing.values()) {
            // Given: Instance with specific facing
            Transform3d transform = new Transform3d(new Vec3d(0, 0, 0), facing);
            OpeningInstance original = OpeningInstance.builder(OpeningId.parse("aperture:test"))
                .transform(transform)
                .build();

            // When: Round-trip through NBT
            var nbt = new net.minecraft.nbt.CompoundTag();
            var output = ValueOutput.create(nbt, NbtOps.INSTANCE);
            OpeningInstanceNbtCodec.write(output, original);

            var input = ValueInput.create(nbt, NbtOps.INSTANCE);
            OpeningInstance restored = OpeningInstanceNbtCodec.read(input);

            // Then: Facing preserved
            assertEquals(facing, restored.transform().facing(),
                "Facing should be preserved: " + facing);
        }
    }

    @Test
    void roundTrip_openState_preservesOpenRatio() {
        // Given: Instance with open state
        OpeningInstance original = OpeningInstance.builder(OpeningId.parse("aperture:test"))
            .state(new OpeningState(0.75))
            .build();

        // When: Round-trip through NBT
        var nbt = new net.minecraft.nbt.CompoundTag();
        var output = ValueOutput.create(nbt, NbtOps.INSTANCE);
        OpeningInstanceNbtCodec.write(output, original);

        var input = ValueInput.create(nbt, NbtOps.INSTANCE);
        OpeningInstance restored = OpeningInstanceNbtCodec.read(input);

        // Then: Open ratio preserved
        assertEquals(0.75, restored.state().openRatio(), 0.001);
    }
}
