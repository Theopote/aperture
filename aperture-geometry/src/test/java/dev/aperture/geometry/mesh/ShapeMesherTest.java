package dev.aperture.geometry.mesh;

import dev.aperture.math.Vec3d;
import dev.aperture.geometry.ops.ExtrudeOp;
import dev.aperture.geometry.profile.BuiltinProfiles;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShapeMesherTest {
	@Test
	void extrudedProfileProducesCapsAndSides() {
		var extrusion = ExtrudeOp.linear(
			BuiltinProfiles.frameRect(50, 50),
			new Vec3d(0, 0, 0),
			new Vec3d(1000, 0, 0),
			new Vec3d(0, 1, 0),
			new Vec3d(0, 0, 1)
		);

		Mesh mesh = ShapeMesher.meshLocal(extrusion);

		assertEquals(1000, mesh.bounds().width(), 0.01);
		assertEquals(50, mesh.bounds().height(), 0.01);
		assertEquals(50, mesh.bounds().depth(), 0.01);
		assertEquals(12, mesh.triangleCount());
	}

	@Test
	void boxMeshProducesTwelveTriangles() {
		Mesh mesh = ShapeMesher.meshLocal(new dev.aperture.geometry.shape.BoxShape(
			dev.aperture.math.BoundingBox.fromSize(100, 200, 30)
		));

		assertEquals(12, mesh.triangleCount());
	}
}
