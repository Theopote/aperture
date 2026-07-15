package dev.aperture.geometry.model;

import dev.aperture.core.geometry.BoundingBox;

/**
 * A single generated solid belonging to a material slot and render layer.
 */
public record GeometrySolid(
	String componentPath,
	String materialSlot,
	GeometryLayer layer,
	BoundingBox bounds
) {
}
