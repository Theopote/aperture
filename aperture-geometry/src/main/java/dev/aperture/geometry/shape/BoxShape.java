package dev.aperture.geometry.shape;

import dev.aperture.core.geometry.BoundingBox;

/**
 * Axis-aligned box solid.
 */
public record BoxShape(BoundingBox bounds) implements SolidShape {
	public BoxShape {
		if (bounds.width() <= 0 || bounds.height() <= 0 || bounds.depth() <= 0) {
			throw new IllegalArgumentException("box dimensions must be positive");
		}
	}
}
