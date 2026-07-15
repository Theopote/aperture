package dev.aperture.geometry.mesh;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec2d;
import dev.aperture.core.geometry.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates interleaved mesh vertices and triangle indices.
 */
public final class MeshBuilder {
	private final List<Float> vertices = new ArrayList<>();
	private final List<Integer> indices = new ArrayList<>();
	private double minX = Double.POSITIVE_INFINITY;
	private double minY = Double.POSITIVE_INFINITY;
	private double minZ = Double.POSITIVE_INFINITY;
	private double maxX = Double.NEGATIVE_INFINITY;
	private double maxY = Double.NEGATIVE_INFINITY;
	private double maxZ = Double.NEGATIVE_INFINITY;

	public void addTriangle(Vec3d a, Vec3d b, Vec3d c, Vec2d uvA, Vec2d uvB, Vec2d uvC) {
		Vec3d normal = b.subtract(a).cross(c.subtract(a)).normalize();
		int baseIndex = vertices.size() / Mesh.FLOATS_PER_VERTEX;
		appendVertex(a, normal, uvA);
		appendVertex(b, normal, uvB);
		appendVertex(c, normal, uvC);
		indices.add(baseIndex);
		indices.add(baseIndex + 1);
		indices.add(baseIndex + 2);
	}

	public void addQuad(Vec3d a, Vec3d b, Vec3d c, Vec3d d, Vec2d uvA, Vec2d uvB, Vec2d uvC, Vec2d uvD) {
		addTriangle(a, b, c, uvA, uvB, uvC);
		addTriangle(a, c, d, uvA, uvC, uvD);
	}

	public boolean isEmpty() {
		return vertices.isEmpty();
	}

	public Mesh build() {
		if (vertices.isEmpty()) {
			throw new IllegalStateException("mesh has no vertices");
		}
		float[] vertexArray = new float[vertices.size()];
		for (int i = 0; i < vertices.size(); i++) {
			vertexArray[i] = vertices.get(i);
		}
		int[] indexArray = indices.stream().mapToInt(Integer::intValue).toArray();
		return new Mesh(vertexArray, indexArray, new BoundingBox(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ)));
	}

	private void appendVertex(Vec3d position, Vec3d normal, Vec2d uv) {
		expandBounds(position);
		vertices.add((float) position.x());
		vertices.add((float) position.y());
		vertices.add((float) position.z());
		vertices.add((float) normal.x());
		vertices.add((float) normal.y());
		vertices.add((float) normal.z());
		vertices.add((float) uv.u());
		vertices.add((float) uv.v());
	}

	private void expandBounds(Vec3d position) {
		minX = Math.min(minX, position.x());
		minY = Math.min(minY, position.y());
		minZ = Math.min(minZ, position.z());
		maxX = Math.max(maxX, position.x());
		maxY = Math.max(maxY, position.y());
		maxZ = Math.max(maxZ, position.z());
	}
}
