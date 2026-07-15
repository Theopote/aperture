package dev.aperture.geometry.shape;

import dev.aperture.core.geometry.BoundingBox;

/**
 * Platform-agnostic solid shape description produced by opening generators.
 */
public sealed interface SolidShape permits BoxShape, ExtrusionShape, UnionShape, SubtractShape {
	BoundingBox bounds();
}
