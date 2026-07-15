package dev.aperture.fabric.placement;

import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.HostType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Classifies a raycast hit as a structural host and builds its {@link HostBinding}.
 */
public final class HostClassifier {
	private HostClassifier() {
	}

	public static boolean isHostBlock(BlockState state) {
		return !state.isAir() && state.canOcclude();
	}

	public static HostType classifyHostType(Direction face) {
		return switch (face.getAxis()) {
			case Y -> face == Direction.UP ? HostType.ROOF : HostType.WALL;
			case X, Z -> HostType.WALL;
		};
	}

	public static HostBinding createBinding(Level level, BlockPos hitPos, Direction face, BlockPos regionMin, BlockPos regionMax) {
		if (!isHostBlock(level.getBlockState(hitPos))) {
			throw new IllegalArgumentException("Hit block is not a valid host at " + hitPos);
		}

		HostType type = classifyHostType(face);
		String anchor = HostAnchor.formatRegion(regionMin, regionMax, face);
		return new HostBinding(type, anchor);
	}
}
