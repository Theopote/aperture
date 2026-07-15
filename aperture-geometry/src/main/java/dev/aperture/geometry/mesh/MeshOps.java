package dev.aperture.geometry.mesh;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Mesh-level constructive solid geometry helpers.
 */
public final class MeshOps {
	private MeshOps() {
	}

	public static Mesh union(Mesh left, Mesh right) {
		float[] vertices = new float[left.vertices().length + right.vertices().length];
		System.arraycopy(left.vertices(), 0, vertices, 0, left.vertices().length);
		System.arraycopy(right.vertices(), 0, vertices, left.vertices().length, right.vertices().length);

		int leftVertexCount = left.vertexCount();
		int[] indices = new int[left.indices().length + right.indices().length];
		System.arraycopy(left.indices(), 0, indices, 0, left.indices().length);
		for (int i = 0; i < right.indices().length; i++) {
			indices[left.indices().length + i] = right.indices()[i] + leftVertexCount;
		}

		return new Mesh(vertices, indices, left.bounds().union(right.bounds()));
	}

	public static Mesh unionAll(List<Mesh> meshes) {
		if (meshes.isEmpty()) {
			throw new IllegalArgumentException("meshes must not be empty");
		}
		Mesh merged = meshes.getFirst();
		for (int i = 1; i < meshes.size(); i++) {
			merged = union(merged, meshes.get(i));
		}
		return merged;
	}

	public static Mesh subtractByBounds(Mesh mesh, BoundingBox tool) {
		List<Float> vertices = new ArrayList<>();
		List<Integer> indices = new ArrayList<>();
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;

		for (int triangle = 0; triangle < mesh.triangleCount(); triangle++) {
			int indexOffset = triangle * 3;
			Vec3d a = readPosition(mesh.vertices(), mesh.indices()[indexOffset]);
			Vec3d b = readPosition(mesh.vertices(), mesh.indices()[indexOffset + 1]);
			Vec3d c = readPosition(mesh.vertices(), mesh.indices()[indexOffset + 2]);
			Vec3d centroid = new Vec3d(
				(a.x() + b.x() + c.x()) / 3.0,
				(a.y() + b.y() + c.y()) / 3.0,
				(a.z() + b.z() + c.z()) / 3.0
			);
			if (containsCentroid(tool, centroid)) {
				continue;
			}

			int baseIndex = vertices.size() / Mesh.FLOATS_PER_VERTEX;
			appendVertex(vertices, mesh.vertices(), mesh.indices()[indexOffset]);
			appendVertex(vertices, mesh.vertices(), mesh.indices()[indexOffset + 1]);
			appendVertex(vertices, mesh.vertices(), mesh.indices()[indexOffset + 2]);
			indices.add(baseIndex);
			indices.add(baseIndex + 1);
			indices.add(baseIndex + 2);

			for (Vec3d point : List.of(a, b, c)) {
				minX = Math.min(minX, point.x());
				minY = Math.min(minY, point.y());
				minZ = Math.min(minZ, point.z());
				maxX = Math.max(maxX, point.x());
				maxY = Math.max(maxY, point.y());
				maxZ = Math.max(maxZ, point.z());
			}
		}

		if (vertices.isEmpty()) {
			throw new IllegalStateException("subtraction removed all mesh geometry");
		}

		float[] vertexArray = new float[vertices.size()];
		for (int i = 0; i < vertices.size(); i++) {
			vertexArray[i] = vertices.get(i);
		}
		int[] indexArray = indices.stream().mapToInt(Integer::intValue).toArray();
		return new Mesh(vertexArray, indexArray, new BoundingBox(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ)));
	}

	private static boolean containsCentroid(BoundingBox box, Vec3d point) {
		return point.x() >= box.min().x() && point.x() <= box.max().x()
			&& point.y() >= box.min().y() && point.y() <= box.max().y()
			&& point.z() >= box.min().z() && point.z() <= box.max().z();
	}

	private static Vec3d readPosition(float[] vertices, int vertexIndex) {
		int offset = vertexIndex * Mesh.FLOATS_PER_VERTEX;
		return new Vec3d(vertices[offset], vertices[offset + 1], vertices[offset + 2]);
	}

	private static void appendVertex(List<Float> target, float[] source, int vertexIndex) {
		int offset = vertexIndex * Mesh.FLOATS_PER_VERTEX;
		for (int i = 0; i < Mesh.FLOATS_PER_VERTEX; i++) {
			target.add(source[offset + i]);
		}
	}
}
