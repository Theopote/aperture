package dev.aperture.geometry.shape;

import dev.aperture.math.BoundingBox;

/**
 * Boolean subtraction of one solid shape from another.
 */
public record SubtractShape(SolidShape base, SolidShape tool) implements SolidShape {
	public SubtractShape {
		if (base == null || tool == null) {
			throw new IllegalArgumentException("base and tool must not be null");
		}
	}

	@Override
	public BoundingBox bounds() {
		return base.bounds();
	}
}
