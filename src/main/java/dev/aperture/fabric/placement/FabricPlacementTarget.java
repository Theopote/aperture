package dev.aperture.fabric.placement;

import dev.aperture.geometry.primitives.BoundingBox;
import dev.aperture.geometry.primitives.Facing;
import dev.aperture.geometry.primitives.Transform3d;
import dev.aperture.geometry.primitives.Vec3d;
import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.placement.PlacementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Result of resolving a Fabric raycast hit into core placement data.
 */
public record FabricPlacementTarget(
	BlockPos hitPos,
	Direction hitFace,
	HostBinding host,
	BoundingBox hostBounds,
	PlacementContext placementContext,
	Transform3d suggestedTransform
) {
}
