package dev.aperture.fabric.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Scans a coplanar host region from a raycast hit to determine available opening bounds.
 */
public final class HostPlaneScanner {
	private static final int MAX_PLANE_EXPANSION = 32;
	private static final int MAX_DEPTH_EXPANSION = 8;

	private HostPlaneScanner() {
	}

	public static HostRegion scan(Level level, BlockPos hitPos, Direction face) {
		PlaneAxes axes = PlaneAxes.forFace(face);

		int minU = 0;
		int maxU = 0;
		int minV = 0;
		int maxV = 0;

		while (minU > -MAX_PLANE_EXPANSION && isPlaneUSliceSolid(level, hitPos, axes, minU - 1, minV, maxV)) {
			minU--;
		}
		while (maxU < MAX_PLANE_EXPANSION && isPlaneUSliceSolid(level, hitPos, axes, maxU + 1, minV, maxV)) {
			maxU++;
		}
		while (minV > -MAX_PLANE_EXPANSION && isPlaneVSliceSolid(level, hitPos, axes, minU, maxU, minV - 1)) {
			minV--;
		}
		while (maxV < MAX_PLANE_EXPANSION && isPlaneVSliceSolid(level, hitPos, axes, minU, maxU, maxV + 1)) {
			maxV++;
		}

		int minDepth = 0;
		int maxDepth = 0;
		while (minDepth > -MAX_DEPTH_EXPANSION && depthSliceSolid(level, hitPos, axes, minU, maxU, minV, maxV, minDepth - 1)) {
			minDepth--;
		}
		while (maxDepth < MAX_DEPTH_EXPANSION && depthSliceSolid(level, hitPos, axes, minU, maxU, minV, maxV, maxDepth + 1)) {
			maxDepth++;
		}

		BlockPos regionMin = offset(hitPos, axes, minU, minV, minDepth);
		BlockPos regionMax = offset(hitPos, axes, maxU, maxV, maxDepth);
		return new HostRegion(regionMin, regionMax, face);
	}

	private static boolean isPlaneUSliceSolid(
		Level level,
		BlockPos origin,
		PlaneAxes axes,
		int u,
		int minV,
		int maxV
	) {
		for (int v = minV; v <= maxV; v++) {
			if (!isHostBlock(level.getBlockState(offset(origin, axes, u, v, 0)))) {
				return false;
			}
		}
		return true;
	}

	private static boolean isPlaneVSliceSolid(
		Level level,
		BlockPos origin,
		PlaneAxes axes,
		int minU,
		int maxU,
		int v
	) {
		for (int u = minU; u <= maxU; u++) {
			if (!isHostBlock(level.getBlockState(offset(origin, axes, u, v, 0)))) {
				return false;
			}
		}
		return true;
	}

	private static boolean depthSliceSolid(
		Level level,
		BlockPos origin,
		PlaneAxes axes,
		int minU,
		int maxU,
		int minV,
		int maxV,
		int depth
	) {
		for (int u = minU; u <= maxU; u++) {
			for (int v = minV; v <= maxV; v++) {
				if (!isHostBlock(level.getBlockState(offset(origin, axes, u, v, depth)))) {
					return false;
				}
			}
		}
		return true;
	}

	private static BlockPos offset(BlockPos origin, PlaneAxes axes, int u, int v, int depth) {
		BlockPos.MutableBlockPos cursor = origin.mutable();
		cursor.move(axes.u(), u);
		cursor.move(axes.v(), v);
		cursor.move(axes.depth(), depth);
		return cursor.immutable();
	}

	private static boolean isHostBlock(BlockState state) {
		return HostClassifier.isHostBlock(state);
	}

	public record HostRegion(BlockPos minInclusive, BlockPos maxInclusive, Direction face) {
	}

	private record PlaneAxes(Direction u, Direction v, Direction depth) {
		static PlaneAxes forFace(Direction face) {
			return switch (face.getAxis()) {
				case X -> new PlaneAxes(Direction.UP, Direction.SOUTH, face);
				case Z -> new PlaneAxes(Direction.EAST, Direction.UP, face);
				case Y -> new PlaneAxes(Direction.EAST, Direction.SOUTH, face);
			};
		}
	}
}
