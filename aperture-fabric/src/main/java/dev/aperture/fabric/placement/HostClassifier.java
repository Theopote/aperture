package dev.aperture.fabric.placement;

import dev.aperture.core.instance.HostAttachmentMode;
import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.HostFeatureId;
import dev.aperture.core.instance.HostType;
import dev.aperture.core.object.ArchitecturalObjectId;
import dev.aperture.math.LocalFrame;
import dev.aperture.math.Vec3d;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import dev.aperture.parameter.ParameterSet;
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

		String anchor = HostAnchor.formatRegion(regionMin, regionMax, face);
		ArchitecturalObjectId hostId = ArchitecturalObjectId.deterministic(
			"minecraft:" + level.dimension().identifier() + ":" + anchor
		);
		Vec3d origin = new Vec3d(
			McUnits.blocksToMillimeters(hitPos.getX() + 0.5 + face.getStepX() * 0.5),
			McUnits.blocksToMillimeters(hitPos.getY() + 0.5 + face.getStepY() * 0.5),
			McUnits.blocksToMillimeters(hitPos.getZ() + 0.5 + face.getStepZ() * 0.5)
		);
		Vec3d normal = new Vec3d(face.getStepX(), face.getStepY(), face.getStepZ());
		return new HostBinding(hostId, HostFeatureId.face(anchor),
			LocalFrame.fromSurfaceNormal(origin, normal), HostAttachmentMode.CUT_THROUGH,
			ParameterSet.empty(), 0L
		);
	}
}
