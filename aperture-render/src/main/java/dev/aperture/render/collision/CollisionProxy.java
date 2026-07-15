package dev.aperture.render.collision;

import dev.aperture.math.BoundingBox;
import dev.aperture.geometry.model.PartId;

import java.util.List;

/**
 * Simplified collision shape set derived from geometry, independent of render meshes.
 */
public record CollisionProxy(
	List<CollisionVolume> volumes
) {
	public CollisionProxy {
		volumes = List.copyOf(volumes);
	}

	public record CollisionVolume(
		PartId partId,
		BoundingBox bounds,
		CollisionLayer layer
	) {
	}
}
