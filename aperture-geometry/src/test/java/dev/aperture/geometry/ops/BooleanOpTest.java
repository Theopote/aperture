package dev.aperture.geometry.ops;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;
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
		Mesh trimmed = ShapeMesher.meshLocal(mitered);

		assertTrue(trimmed.triangleCount() < full.triangleCount());
	}

	@Test
	void unionCombinesTwoBoxes() {
		var left = new BoxShape(BoundingBox.fromSize(100, 100, 50));
		var right = new BoxShape(new BoundingBox(new Vec3d(100, 0, 0), new Vec3d(200, 100, 50)));
		Mesh merged = ShapeMesher.meshLocal(BooleanOp.union(left, right));

		assertEquals(24, merged.triangleCount());
	}
}
