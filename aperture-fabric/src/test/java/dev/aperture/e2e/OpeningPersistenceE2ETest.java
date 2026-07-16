package dev.aperture.e2e;

import dev.aperture.block.OpeningBlock;
import dev.aperture.block.entity.OpeningBlockEntity;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * End-to-end tests for Opening persistence using Minecraft GameTest framework.
 *
 * These tests verify the complete lifecycle:
 * 1. Place OpeningBlock with OpeningInstance
 * 2. BlockEntity stores instance data
 * 3. Save to NBT (simulated via getUpdateTag/load)
 * 4. Load from NBT
 * 5. Verify all data intact
 */
public class OpeningPersistenceE2ETest {

    /**
     * Basic test: place door, verify it persists through NBT round-trip.
     */
    @GameTest(template = "aperture:empty_3x3x3")
    public static void doorPersistence_basicRoundTrip(GameTestHelper helper) {
        // Given: A door opening instance
        UUID instanceId = UUID.randomUUID();
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(900.0))
            .put("height", ParameterValue.length(2100.0))
            .put("panel_count", ParameterValue.count(1))
            .build();

        OpeningInstance door = new OpeningInstance(
            1,
            instanceId,
            "aperture:door",
            params,
            null,  // transform
            null,  // host
            null,  // state
            0      // revision
        );

        // When: Place opening block
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, OpeningBlock.INSTANCE.defaultBlockState());

        BlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(be instanceof OpeningBlockEntity, "Should be OpeningBlockEntity");

        OpeningBlockEntity obe = (OpeningBlockEntity) be;
        obe.setInstance(door);

        // Simulate save/load via NBT
        var saveTag = obe.getUpdateTag();

        // Create new block entity and load
        helper.setBlock(pos, Blocks.AIR.defaultBlockState());
        helper.setBlock(pos, OpeningBlock.INSTANCE.defaultBlockState());

        BlockEntity be2 = helper.getBlockEntity(pos);
        helper.assertTrue(be2 instanceof OpeningBlockEntity, "Should be OpeningBlockEntity after reload");

        OpeningBlockEntity obe2 = (OpeningBlockEntity) be2;
        obe2.load(saveTag);

        // Then: Verify data intact
        OpeningInstance loaded = obe2.getInstance();
        helper.assertNotNull(loaded, "Instance should not be null after load");
        helper.assertTrue(loaded.instanceId().equals(instanceId), "Instance ID should match");
        helper.assertTrue(loaded.typeId().equals("aperture:door"), "Type ID should match");

        // Verify parameters
        var loadedParams = loaded.parameters();
        helper.assertTrue(loadedParams.has("width"), "Should have width parameter");
        helper.assertTrue(loadedParams.get("width").asLength() == 900.0, "Width should be 900");
        helper.assertTrue(loadedParams.get("height").asLength() == 2100.0, "Height should be 2100");
        helper.assertTrue(loadedParams.get("panel_count").asCount() == 1, "Panel count should be 1");

        helper.succeed();
    }

    /**
     * Test: fixed window persistence.
     */
    @GameTest(template = "aperture:empty_3x3x3")
    public static void fixedWindowPersistence_roundTrip(GameTestHelper helper) {
        // Given: A fixed window
        UUID instanceId = UUID.randomUUID();
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(1200.0))
            .put("height", ParameterValue.length(1500.0))
            .build();

        OpeningInstance window = new OpeningInstance(
            1,
            instanceId,
            "aperture:fixed_window",
            params,
            null, null, null, 0
        );

        // When: Place and save/load
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, OpeningBlock.INSTANCE.defaultBlockState());

        OpeningBlockEntity obe = (OpeningBlockEntity) helper.getBlockEntity(pos);
        obe.setInstance(window);

        var saveTag = obe.getUpdateTag();

        // Reload
        helper.setBlock(pos, Blocks.AIR.defaultBlockState());
        helper.setBlock(pos, OpeningBlock.INSTANCE.defaultBlockState());

        OpeningBlockEntity obe2 = (OpeningBlockEntity) helper.getBlockEntity(pos);
        obe2.load(saveTag);

        // Then: Verify
        OpeningInstance loaded = obe2.getInstance();
        helper.assertNotNull(loaded, "Instance should exist");
        helper.assertTrue(loaded.typeId().equals("aperture:fixed_window"), "Should be fixed_window");
        helper.assertTrue(loaded.parameters().get("width").asLength() == 1200.0, "Width preserved");
        helper.assertTrue(loaded.parameters().get("height").asLength() == 1500.0, "Height preserved");

        helper.succeed();
    }

    /**
     * Test: Multiple openings in same chunk.
     */
    @GameTest(template = "aperture:empty_5x5x5")
    public static void multiplePersistence_sameChunk(GameTestHelper helper) {
        // Given: Three different openings
        OpeningInstance door = createDoorInstance();
        OpeningInstance window1 = createWindowInstance(1200, 1500);
        OpeningInstance window2 = createWindowInstance(800, 1000);

        // When: Place all three
        BlockPos pos1 = new BlockPos(1, 1, 1);
        BlockPos pos2 = new BlockPos(3, 1, 1);
        BlockPos pos3 = new BlockPos(1, 1, 3);

        placeAndSetInstance(helper, pos1, door);
        placeAndSetInstance(helper, pos2, window1);
        placeAndSetInstance(helper, pos3, window2);

        // Save all
        var tag1 = ((OpeningBlockEntity) helper.getBlockEntity(pos1)).getUpdateTag();
        var tag2 = ((OpeningBlockEntity) helper.getBlockEntity(pos2)).getUpdateTag();
        var tag3 = ((OpeningBlockEntity) helper.getBlockEntity(pos3)).getUpdateTag();

        // Clear and reload
        helper.setBlock(pos1, Blocks.AIR.defaultBlockState());
        helper.setBlock(pos2, Blocks.AIR.defaultBlockState());
        helper.setBlock(pos3, Blocks.AIR.defaultBlockState());

        placeAndLoad(helper, pos1, tag1);
        placeAndLoad(helper, pos2, tag2);
        placeAndLoad(helper, pos3, tag3);

        // Then: All should be intact
        OpeningInstance loaded1 = ((OpeningBlockEntity) helper.getBlockEntity(pos1)).getInstance();
        OpeningInstance loaded2 = ((OpeningBlockEntity) helper.getBlockEntity(pos2)).getInstance();
        OpeningInstance loaded3 = ((OpeningBlockEntity) helper.getBlockEntity(pos3)).getInstance();

        helper.assertTrue(loaded1.typeId().equals("aperture:door"), "Door should persist");
        helper.assertTrue(loaded2.typeId().equals("aperture:fixed_window"), "Window 1 should persist");
        helper.assertTrue(loaded3.typeId().equals("aperture:fixed_window"), "Window 2 should persist");

        helper.assertTrue(loaded2.parameters().get("width").asLength() == 1200.0, "Window 1 size preserved");
        helper.assertTrue(loaded3.parameters().get("width").asLength() == 800.0, "Window 2 size preserved");

        helper.succeed();
    }

    /**
     * Test: All parameter types persist correctly.
     */
    @GameTest(template = "aperture:empty_3x3x3")
    public static void allParameterTypes_persist(GameTestHelper helper) {
        // Given: Instance with all parameter types
        ParameterSet params = ParameterSet.builder()
            .put("length_param", ParameterValue.length(1500.0))
            .put("angle_param", ParameterValue.angle(45.0))
            .put("count_param", ParameterValue.count(3))
            .put("ratio_param", ParameterValue.number(0.75))
            .put("bool_param", ParameterValue.bool(true))
            .put("material_param", ParameterValue.materialRef("aperture:oak"))
            .put("profile_param", ParameterValue.profileRef("aperture:frame_l50"))
            .put("enum_param", ParameterValue.enumValue("left"))
            .build();

        OpeningInstance instance = new OpeningInstance(
            1,
            UUID.randomUUID(),
            "aperture:test_opening",
            params,
            null, null, null, 0
        );

        // When: Save and load
        BlockPos pos = new BlockPos(1, 1, 1);
        placeAndSetInstance(helper, pos, instance);

        var tag = ((OpeningBlockEntity) helper.getBlockEntity(pos)).getUpdateTag();

        helper.setBlock(pos, Blocks.AIR.defaultBlockState());
        placeAndLoad(helper, pos, tag);

        // Then: All parameters should be preserved
        OpeningInstance loaded = ((OpeningBlockEntity) helper.getBlockEntity(pos)).getInstance();
        var loadedParams = loaded.parameters();

        helper.assertTrue(loadedParams.get("length_param").asLength() == 1500.0, "Length preserved");
        helper.assertTrue(loadedParams.get("angle_param").asAngle() == 45.0, "Angle preserved");
        helper.assertTrue(loadedParams.get("count_param").asCount() == 3, "Count preserved");
        helper.assertTrue(loadedParams.get("ratio_param").asNumber() == 0.75, "Ratio preserved");
        helper.assertTrue(loadedParams.get("bool_param").asBool() == true, "Bool preserved");
        helper.assertTrue(loadedParams.get("material_param").asMaterialRef().equals("aperture:oak"), "Material preserved");
        helper.assertTrue(loadedParams.get("profile_param").asProfileRef().equals("aperture:frame_l50"), "Profile preserved");
        helper.assertTrue(loadedParams.get("enum_param").asEnumValue().equals("left"), "Enum preserved");

        helper.succeed();
    }

    // Helper methods

    private static OpeningInstance createDoorInstance() {
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(900.0))
            .put("height", ParameterValue.length(2100.0))
            .build();

        return new OpeningInstance(
            1,
            UUID.randomUUID(),
            "aperture:door",
            params,
            null, null, null, 0
        );
    }

    private static OpeningInstance createWindowInstance(double width, double height) {
        ParameterSet params = ParameterSet.builder()
            .put("width", ParameterValue.length(width))
            .put("height", ParameterValue.length(height))
            .build();

        return new OpeningInstance(
            1,
            UUID.randomUUID(),
            "aperture:fixed_window",
            params,
            null, null, null, 0
        );
    }

    private static void placeAndSetInstance(GameTestHelper helper, BlockPos pos, OpeningInstance instance) {
        helper.setBlock(pos, OpeningBlock.INSTANCE.defaultBlockState());
        OpeningBlockEntity obe = (OpeningBlockEntity) helper.getBlockEntity(pos);
        obe.setInstance(instance);
    }

    private static void placeAndLoad(GameTestHelper helper, BlockPos pos, net.minecraft.nbt.CompoundTag tag) {
        helper.setBlock(pos, OpeningBlock.INSTANCE.defaultBlockState());
        OpeningBlockEntity obe = (OpeningBlockEntity) helper.getBlockEntity(pos);
        obe.load(tag);
    }
}
