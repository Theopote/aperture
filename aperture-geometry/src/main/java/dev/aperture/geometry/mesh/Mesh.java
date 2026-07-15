package dev.aperture.geometry.mesh;

import dev.aperture.core.geometry.BoundingBox;

import java.util.Arrays;

/**
 * CPU-side triangle mesh in platform-agnostic space.
 * Vertex layout: position (3) + normal (3) + uv (2) = 8 floats per vertex.
 */
public record Mesh(float[] vertices, int[] indices, BoundingBox bounds) {
	public static final int FLOATS_PER_VERTEX = 8;

	public Mesh {
		if (vertices.length % FLOATS_PER_VERTEX != 0) {
			throw new IllegalArgumentException("vertices length must be a multiple of " + FLOATS_PER_VERTEX);
		}
		if (indices.length % 3 != 0) {
			throw new IllegalArgumentException("indices length must be a multiple of 3");
		}
		vertices = Arrays.copyOf(vertices, vertices.length);
		indices = Arrays.copyOf(indices, indices.length);
	}

	public int vertexCount() {
		return vertices.length / FLOATS_PER_VERTEX;
	}

	public int triangleCount() {
		return indices.length / 3;
	}
}
