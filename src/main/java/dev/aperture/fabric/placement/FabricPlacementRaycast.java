package dev.aperture.fabric.placement;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Performs block raycasts from a viewer entity.
 */
public final class FabricPlacementRaycast {
	private static final double DEFAULT_REACH = 5.0D;

	private FabricPlacementRaycast() {
	}

	public static BlockHitResult raycast(Entity viewer) {
		return raycast(viewer, DEFAULT_REACH);
	}

	public static BlockHitResult raycast(Entity viewer, double reach) {
		Vec3 eye = viewer.getEyePosition();
		Vec3 look = viewer.getViewVector(1.0F);
		Vec3 end = eye.add(look.scale(reach));
		ClipContext context = new ClipContext(
			eye,
			end,
			ClipContext.Block.OUTLINE,
			ClipContext.Fluid.NONE,
			viewer
		);
		return viewer.level().clip(context);
	}

	public static BlockHitResult fromHitResult(HitResult hitResult) {
		if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
			return null;
		}
		return (BlockHitResult) hitResult;
	}

	public static BlockHitResult crosshair(Level level, Entity viewer, HitResult hitResult) {
		BlockHitResult fromClient = fromHitResult(hitResult);
		if (fromClient != null) {
			return fromClient;
		}
		return raycast(viewer);
	}
}
