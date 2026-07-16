package dev.aperture.geometry.model;

import dev.aperture.math.BoundingBox;

import java.util.List;

/**
 * Output of procedural opening generation.
 */
public record GeometryResult(
	List<GeometrySolid> solids,
	BoundingBox bounds,
	BoundingBox cutVolume
) {
	public GeometryResult {
		solids = List.copyOf(solids);
	}

	/**
	 * Returns an empty geometry result (for testing/mocking).
	 */
	public static GeometryResult empty() {
		return new GeometryResult(
			List.of(),
			BoundingBox.EMPTY,
			BoundingBox.EMPTY
		);
	}
}
