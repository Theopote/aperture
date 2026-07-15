package dev.aperture.geometry.mesh;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.ops.ExtrudeOp;
import dev.aperture.geometry.profile.BuiltinProfiles;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeshCsgTest {
	@Test
	void preciseSubtractSplitsTriangleCrossingBox() {
		var extrusion = ExtrudeOp.linear(
			BuiltinProfiles.frameRect(50, 50),
			new Vec3d(0, 0, 0),
			new Vec3d(1000, 0, 0),
			new Vec3d(0, 1, 0),
			new Vec3d(0, 0, 1)
		);
		Mesh full = ShapeMesher.meshLocal(extrusion);
		BoundingBox corner = new BoundingBox(Vec3d.ZERO, new Vec3d(50, 50, 50));

		Mesh precise = MeshCsg.subtractBox(full, corner);
		Mesh coarse = MeshOps.subtractByBounds(full, corner);

		assertTrue(precise.triangleCount() >= coarse.triangleCount());
		assertTrue(precise.triangleCount() > 0);
	}

	@Test
	void triangleFullyOutsideBoxIsPreserved() {
		MeshBuilder builder = new MeshBuilder();
		builder.addTriangle(
			new Vec3d(100, 0, 0),
			new Vec3d(110, 0, 0),
			new Vec3d(105, 10, 0),
			dev.aperture.core.geometry.Vec2d.ZERO,
			dev.aperture.core.geometry.Vec2d.ZERO,
			dev.aperture.core.geometry.Vec2d.ZERO
		);
		Mesh mesh = builder.build();
		Mesh trimmed = MeshCsg.subtractBox(mesh, new BoundingBox(Vec3d.ZERO, new Vec3d(50, 50, 50)));

		assertEquals(1, trimmed.triangleCount());
	}
}
