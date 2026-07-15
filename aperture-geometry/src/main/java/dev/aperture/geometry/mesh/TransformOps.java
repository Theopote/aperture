package dev.aperture.geometry.mesh;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.core.geometry.Transform3d;

/**
 * Applies {@link Transform3d} to mesh vertex data.
 */
public final class TransformOps {
	private TransformOps() {
	}

	public static Mesh apply(Mesh mesh, Transform3d transform) {
		if (transform.origin().equals(Vec3d.ZERO) && !transform.hasRotation()) {
			return mesh;
		}

		float[] vertices = mesh.vertices().clone();
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		double maxZ = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < vertices.length; i += Mesh.FLOATS_PER_VERTEX) {
			Vec3d position = transform.transformPoint(new Vec3d(vertices[i], vertices[i + 1], vertices[i + 2]));
			Vec3d normal = transform.transformDirection(new Vec3d(vertices[i + 3], vertices[i + 4], vertices[i + 5])).normalize();

			vertices[i] = (float) position.x();
			vertices[i + 1] = (float) position.y();
			vertices[i + 2] = (float) position.z();
			vertices[i + 3] = (float) normal.x();
			vertices[i + 4] = (float) normal.y();
			vertices[i + 5] = (float) normal.z();

			minX = Math.min(minX, position.x());
			minY = Math.min(minY, position.y());
			minZ = Math.min(minZ, position.z());
			maxX = Math.max(maxX, position.x());
			maxY = Math.max(maxY, position.y());
			maxZ = Math.max(maxZ, position.z());
		}

		return new Mesh(
			vertices,
			mesh.indices(),
			new BoundingBox(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ))
		);
	}
}
