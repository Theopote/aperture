package dev.aperture.fabric.placement;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Facing;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningInstanceStore;
import dev.aperture.core.placement.PlacementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fabric adapter that turns raycast hits into {@link PlacementContext} and suggested transforms.
 */
public final class FabricPlacementAdapter {
	private static final double INNER_FACE_INSET_MM = 1.0;

	public Optional<FabricPlacementTarget> fromCrosshair(
		Level level,
		Entity viewer,
		HitResult hitResult,
		OpeningInstanceStore instances
	) {
		BlockHitResult blockHit = FabricPlacementRaycast.crosshair(level, viewer, hitResult);
		if (blockHit == null || blockHit.getType() != HitResult.Type.BLOCK) {
			return Optional.empty();
		}
		return fromBlockHit(level, blockHit, instances);
	}

	public Optional<FabricPlacementTarget> fromBlockHit(
		Level level,
		BlockHitResult blockHit,
		OpeningInstanceStore instances
	) {
		if (blockHit.getType() != HitResult.Type.BLOCK) {
			return Optional.empty();
		}

		BlockPos hitPos = blockHit.getBlockPos();
		Direction hitFace = blockHit.getDirection();
		if (!HostClassifier.isHostBlock(level.getBlockState(hitPos))) {
			return Optional.empty();
		}

		HostPlaneScanner.HostRegion region = HostPlaneScanner.scan(level, hitPos, hitFace);
		HostBinding host = HostClassifier.createBinding(
			level,
			hitPos,
			hitFace,
			region.minInclusive(),
			region.maxInclusive()
		);
		BoundingBox hostBounds = McCoordinates.blockRegionToBounds(region.minInclusive(), region.maxInclusive());
		Transform3d suggestedTransform = suggestTransform(region, hitFace);
		PlacementContext context = new PlacementContext(host, hostBounds, findExistingOnHost(instances, host));

		return Optional.of(new FabricPlacementTarget(
			hitPos,
			hitFace,
			host,
			hostBounds,
			context,
			suggestedTransform
		));
	}

	private static List<OpeningInstance> findExistingOnHost(OpeningInstanceStore instances, HostBinding host) {
		List<OpeningInstance> matches = new ArrayList<>();
		for (OpeningInstance instance : instances.all()) {
			if (sameHostRegion(instance.host(), host)) {
				matches.add(instance);
			}
		}
		return matches;
	}

	private static boolean sameHostRegion(HostBinding left, HostBinding right) {
		return left.type() == right.type() && left.anchor().equals(right.anchor());
	}

	private static Transform3d suggestTransform(HostPlaneScanner.HostRegion region, Direction hitFace) {
		BlockPos min = region.minInclusive();
		BlockPos max = region.maxInclusive();
		Facing facing = McFacingConverter.toFacing(hitFace.getOpposite());
		Vec3d origin = innerBottomLeft(min, max, hitFace);
		return new Transform3d(origin, facing);
	}

	private static Vec3d innerBottomLeft(BlockPos min, BlockPos max, Direction hitFace) {
		return switch (hitFace.getAxis()) {
			case Z -> {
				double x = McUnits.blocksToMillimeters(min.getX());
				double y = McUnits.blocksToMillimeters(min.getY());
				double z = hitFace == Direction.NORTH
					? McUnits.blocksToMillimeters(max.getZ() + 1) - INNER_FACE_INSET_MM
					: McUnits.blocksToMillimeters(min.getZ()) + INNER_FACE_INSET_MM;
				yield new Vec3d(x, y, z);
			}
			case X -> {
				double x = hitFace == Direction.EAST
					? McUnits.blocksToMillimeters(min.getX()) + INNER_FACE_INSET_MM
					: McUnits.blocksToMillimeters(max.getX() + 1) - INNER_FACE_INSET_MM;
				double y = McUnits.blocksToMillimeters(min.getY());
				double z = McUnits.blocksToMillimeters(min.getZ());
				yield new Vec3d(x, y, z);
			}
			case Y -> {
				double x = McUnits.blocksToMillimeters(min.getX());
				double y = hitFace == Direction.UP
					? McUnits.blocksToMillimeters(min.getY()) + INNER_FACE_INSET_MM
					: McUnits.blocksToMillimeters(max.getY() + 1) - INNER_FACE_INSET_MM;
				double z = McUnits.blocksToMillimeters(min.getZ());
				yield new Vec3d(x, y, z);
			}
		};
	}
}
