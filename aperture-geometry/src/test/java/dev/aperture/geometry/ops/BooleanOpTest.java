package dev.aperture.geometry.ops;

import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.mesh.ShapeMesher;
import dev.aperture.geometry.profile.BuiltinProfiles;
import dev.aperture.geometry.shape.BoxShape;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BooleanOpTest {
	@Test
	void subtractBoxRemovesCornerTriangles() {
		var extrusion = ExtrudeOp.linear(
			BuiltinProfiles.frameRect(50, 50),
			new Vec3d(0, 0, 0),
			new Vec3d(1000, 0, 0),
			new Vec3d(0, 1, 0),
			new Vec3d(0, 0, 1)
		);
		Mesh full = ShapeMesher.meshLocal(extrusion);

		var mitered = BooleanOp.subtractBox(
			extrusion,
			new BoundingBox(Vec3d.ZERO, new Vec3d(50, 50, 50))
		);
		BoundingBox corner = new BoundingBox(Vec3d.ZERO, new Vec3d(50, 50, 50));
		Mesh trimmed = ShapeMesher.meshLocal(mitered);

		assertTrue(countTrianglesInside(trimmed, corner) < countTrianglesInside(full, corner));
	}

	private static int countTrianglesInside(Mesh mesh, BoundingBox box) {
		int count = 0;
		for (int triangle = 0; triangle < mesh.triangleCount(); triangle++) {
			int index = triangle * 3;
			Vec3d a = readPosition(mesh, mesh.indices()[index]);
			Vec3d b = readPosition(mesh, mesh.indices()[index + 1]);
			Vec3d c = readPosition(mesh, mesh.indices()[index + 2]);
			Vec3d centroid = new Vec3d(
				(a.x() + b.x() + c.x()) / 3.0,
				(a.y() + b.y() + c.y()) / 3.0,
				(a.z() + b.z() + c.z()) / 3.0
			);
			if (isInside(centroid, box)) {
				count++;
			}
		}
		return count;
	}

	private static boolean isInside(Vec3d point, BoundingBox box) {
		return point.x() >= box.min().x() && point.x() <= box.max().x()
			&& point.y() >= box.min().y() && point.y() <= box.max().y()
			&& point.z() >= box.min().z() && point.z() <= box.max().z();
	}

	private static Vec3d readPosition(Mesh mesh, int vertexIndex) {
		int offset = vertexIndex * Mesh.FLOATS_PER_VERTEX;
		float[] vertices = mesh.vertices();
		return new Vec3d(vertices[offset], vertices[offset + 1], vertices[offset + 2]);
	}

	@Test
	void unionCombinesTwoBoxes() {
		var left = new BoxShape(BoundingBox.fromSize(100, 100, 50));
		var right = new BoxShape(new BoundingBox(new Vec3d(100, 0, 0), new Vec3d(200, 100, 50)));
		Mesh merged = ShapeMesher.meshLocal(BooleanOp.union(left, right));

		assertEquals(24, merged.triangleCount());
	}
}
