package dev.aperture.render.mesh;

import dev.aperture.math.BoundingBox;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.PartId;

import java.util.Arrays;
import java.util.Objects;

/**
 * One draw unit within a mesh asset.
 */
public record MeshSection(
	PartId partId,
	GeometryLayer layer,
	float[] vertices,
	int[] indices,
	BoundingBox bounds,
	MeshHandle handle
) {
	public MeshSection {
		Objects.requireNonNull(partId, "partId");
		Objects.requireNonNull(layer, "layer");
		Objects.requireNonNull(bounds, "bounds");
		Objects.requireNonNull(handle, "handle");
		vertices = Arrays.copyOf(vertices, vertices.length);
		indices = Arrays.copyOf(indices, indices.length);
		VertexFormat.vertexCount(vertices);
		if (indices.length % 3 != 0) {
			throw new IllegalArgumentException("indices length must be a multiple of 3");
		}
	}

	public int triangleCount() {
		return indices.length / 3;
	}
}
