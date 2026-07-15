package dev.aperture.fabric.placement;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Facing;
import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.geometry.Vec3d;
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
