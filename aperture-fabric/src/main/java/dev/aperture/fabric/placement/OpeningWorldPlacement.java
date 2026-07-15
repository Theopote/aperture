package dev.aperture.fabric.placement;

import dev.aperture.block.entity.OpeningBlockEntity;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.registry.ApertureBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Places the invisible opening anchor block for a committed instance.
 */
public final class OpeningWorldPlacement {
	private static final Logger LOGGER = LoggerFactory.getLogger("aperture/fabric/placement");

	private OpeningWorldPlacement() {
	}

	public static void placeCommittedInstance(Level level, OpeningInstance instance) {
		BlockPos pos = transformToBlockPos(instance.transform());
		BlockState state = ApertureBlocks.OPENING.defaultBlockState();
		if (!level.setBlock(pos, state, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS)) {
			LOGGER.warn("Failed to place opening block at {}", pos);
			return;
		}

		if (level.getBlockEntity(pos) instanceof OpeningBlockEntity openingBlockEntity) {
			openingBlockEntity.setInstanceId(instance.instanceId());
		}
	}

	private static BlockPos transformToBlockPos(Transform3d transform) {
		Vec3d origin = transform.origin();
		return BlockPos.containing(
			origin.x() / McUnits.MILLIMETERS_PER_BLOCK,
			origin.y() / McUnits.MILLIMETERS_PER_BLOCK,
			origin.z() / McUnits.MILLIMETERS_PER_BLOCK
		);
	}
}
