package dev.aperture.opening.geometry.kinematic;

import dev.aperture.core.geometry.Transform3d;
import dev.aperture.core.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanelKinematicsTest {
	@Test
	void leftHingeRotationMovesPointOutwardInZ() {
		Transform3d transform = PanelKinematics.solve("left", 50, 1200, 1500, 90);
		Vec3d outerEdge = transform.transformPoint(new Vec3d(1150, 750, 0));

		assertTrue(transform.hasRotation());
		assertTrue(outerEdge.z() > 0);
	}

	@Test
	void zeroAngleProducesIdentityTransform() {
		Transform3d transform = PanelKinematics.solve("left", 50, 1200, 1500, 0);

		assertFalse(transform.hasRotation());
	}
}
