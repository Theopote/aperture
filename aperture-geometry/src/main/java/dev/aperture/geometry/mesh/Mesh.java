package dev.aperture.geometry.mesh;

import dev.aperture.math.BoundingBox;

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

	/** Compatibility alias for callers that treat each triangle as a face. */
	public int faceCount() {
		return triangleCount();
	}

	/** Returns the position of a vertex by index. */
	public dev.aperture.math.Vec3d vertex(int index) {
		if (index < 0 || index >= vertexCount()) {
			throw new IndexOutOfBoundsException(index);
		}
		int offset = index * FLOATS_PER_VERTEX;
		return new dev.aperture.math.Vec3d(vertices[offset], vertices[offset + 1], vertices[offset + 2]);
	}
	public dev.aperture.math.Vec2d uv(int index) {
		if (index < 0 || index >= vertexCount()) {
			throw new IndexOutOfBoundsException(index);
		}
		int offset = index * FLOATS_PER_VERTEX;
		return new dev.aperture.math.Vec2d(vertices[offset + 6], vertices[offset + 7]);
	}

	public int faceVertex(int faceIndex, int cornerIndex) {
		if (faceIndex < 0 || faceIndex >= faceCount()) {
			throw new IndexOutOfBoundsException(faceIndex);
		}
		if (cornerIndex < 0 || cornerIndex >= 3) {
			throw new IndexOutOfBoundsException(cornerIndex);
		}
		return indices[faceIndex * 3 + cornerIndex];
	}}
