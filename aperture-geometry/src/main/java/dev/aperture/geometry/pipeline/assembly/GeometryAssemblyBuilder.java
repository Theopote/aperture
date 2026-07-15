package dev.aperture.geometry.pipeline.assembly;

import dev.aperture.math.BoundingBox;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable builder for assembling {@link GeometryResult} from pipeline generators.
 */
public final class GeometryAssemblyBuilder {
	private final List<GeometrySolid> solids = new ArrayList<>();
	private BoundingBox bounds;
	private BoundingBox cutVolume;

	public void addSolid(GeometrySolid solid) {
		solids.add(solid);
		bounds = bounds == null ? solid.bounds() : bounds.union(solid.bounds());
	}

	public void setCutVolume(BoundingBox cutVolume) {
		this.cutVolume = cutVolume;
	}

	public GeometryResult build() {
		if (solids.isEmpty()) {
			throw new IllegalStateException("generation produced no solids");
		}
		if (bounds == null || cutVolume == null) {
			throw new IllegalStateException("generation bounds or cut volume were not set");
		}
		return new GeometryResult(List.copyOf(solids), bounds, cutVolume);
	}
}
