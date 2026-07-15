package dev.aperture.render.mesh;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.render.data.PartId;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable mesh container for one document at one LOD tier.
 * Updated via {@link #patch(MeshAsset, Map, Set)} rather than in-place mutation.
 */
public record MeshAsset(
	LODLevel level,
	Map<PartId, MeshSection> sections,
	BoundingBox bounds
) {
	public MeshAsset {
		sections = Collections.unmodifiableMap(new LinkedHashMap<>(sections));
	}

	public static MeshAsset empty(LODLevel level) {
		return new MeshAsset(level, Map.of(), BoundingBox.fromSize(0, 0, 0));
	}

	public Optional<MeshSection> section(PartId partId) {
		return Optional.ofNullable(sections.get(partId));
	}

	public Set<PartId> partIds() {
		return sections.keySet();
	}

	/**
	 * Returns a new asset with updated sections and removed parts applied.
	 */
	public static MeshAsset patch(MeshAsset existing, Map<PartId, MeshSection> updatedSections, Set<PartId> removed) {
		Map<PartId, MeshSection> merged = new LinkedHashMap<>(existing.sections);
		for (PartId removedId : removed) {
			merged.remove(removedId);
		}
		merged.putAll(updatedSections);
		BoundingBox bounds = computeBounds(merged);
		return new MeshAsset(existing.level, merged, bounds);
	}

	private static BoundingBox computeBounds(Map<PartId, MeshSection> sections) {
		if (sections.isEmpty()) {
			return BoundingBox.fromSize(0, 0, 0);
		}
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;

		for (MeshSection section : sections.values()) {
			var min = section.bounds().min();
			var max = section.bounds().max();
			minX = Math.min(minX, min.x());
			minY = Math.min(minY, min.y());
			minZ = Math.min(minZ, min.z());
			maxX = Math.max(maxX, max.x());
			maxY = Math.max(maxY, max.y());
			maxZ = Math.max(maxZ, max.z());
		}

		return new BoundingBox(
			new dev.aperture.core.geometry.Vec3d(minX, minY, minZ),
			new dev.aperture.core.geometry.Vec3d(maxX, maxY, maxZ)
		);
	}
}
