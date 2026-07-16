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

	public GeometryResult(List<GeometrySolid> solids) {
		this(solids, combinedBounds(solids), BoundingBox.EMPTY);
	}

	private static BoundingBox combinedBounds(List<GeometrySolid> solids) {
		if (solids.isEmpty()) {
			return BoundingBox.EMPTY;
		}
		BoundingBox bounds = solids.get(0).bounds();
		for (int i = 1; i < solids.size(); i++) {
			bounds = bounds.union(solids.get(i).bounds());
		}
		return bounds;
	}}
