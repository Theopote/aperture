package dev.aperture.render.data;

import dev.aperture.geometry.model.PartId;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Compares geometry snapshots and classifies parts for incremental mesh updates.
 */
public final class RenderDeltaEngine {
	private RenderDeltaEngine() {
	}

	public static RenderDelta compute(GeometryResult previous, GeometryResult next) {
		Map<PartId, GeometrySolid> oldParts = index(previous);
		Map<PartId, GeometrySolid> newParts = index(next);

		Set<PartId> added = new LinkedHashSet<>();
		Set<PartId> removed = new LinkedHashSet<>();
		Set<PartId> changed = new LinkedHashSet<>();
		Set<PartId> unchanged = new LinkedHashSet<>();

		for (PartId id : newParts.keySet()) {
			GeometrySolid oldSolid = oldParts.get(id);
			if (oldSolid == null) {
				added.add(id);
			} else if (!oldSolid.equals(newParts.get(id))) {
				changed.add(id);
			} else {
				unchanged.add(id);
			}
		}

		for (PartId id : oldParts.keySet()) {
			if (!newParts.containsKey(id)) {
				removed.add(id);
			}
		}

		return new RenderDelta(added, removed, changed, unchanged);
	}

	private static Map<PartId, GeometrySolid> index(GeometryResult geometry) {
		Map<PartId, GeometrySolid> indexed = new HashMap<>();
		if (geometry == null) {
			return indexed;
		}
		for (GeometrySolid solid : geometry.solids()) {
			PartId id = PartId.of(solid.componentPath());
			if (indexed.put(id, solid) != null) {
				throw new IllegalStateException("Duplicate componentPath in GeometryResult: " + id.componentPath());
			}
		}
		return indexed;
	}
}
