package dev.aperture.render.collision;

import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.render.data.PartId;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds collision proxies from geometry output without referencing GPU mesh data.
 */
public final class CollisionProxyBuilder {
	private CollisionProxyBuilder() {
	}

	public static CollisionProxy fromGeometry(GeometryResult geometry) {
		List<CollisionProxy.CollisionVolume> volumes = new ArrayList<>();
		for (GeometrySolid solid : geometry.solids()) {
			CollisionLayer layer = mapLayer(solid.layer());
			volumes.add(new CollisionProxy.CollisionVolume(
				PartId.of(solid.componentPath()),
				solid.bounds(),
				layer
			));
		}
		return new CollisionProxy(volumes);
	}

	private static CollisionLayer mapLayer(GeometryLayer layer) {
		return switch (layer) {
			case OPAQUE_FRAME, CUTOUT_HARDWARE -> CollisionLayer.SOLID;
			case TRANSLUCENT_GLASS -> CollisionLayer.OPEN;
		};
	}
}
