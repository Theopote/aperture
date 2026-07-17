package dev.aperture.runtime.model.event;

import dev.aperture.math.Vec3d;

import java.util.Objects;

/** World-relative point plus an optional semantic feature identifier. */
public record SpatialRef(WorldRef world, Vec3d position, String featureId) {
	public SpatialRef {
		Objects.requireNonNull(world, "world");
		Objects.requireNonNull(position, "position");
		featureId = featureId == null ? "" : featureId;
	}
}
