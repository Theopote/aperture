package dev.aperture.opening.runtime;

import dev.aperture.geometry.kinematic.KinematicEvaluator;
import dev.aperture.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DoorKinematicsTest {
	@Test
	void swingPanelUsesOpenRatioAndHingePivot() {
		var part = DoorKinematics.swingPanel("panel.main", new Vec3d(-500, 0, 0), true);
		var transform = KinematicEvaluator.evaluate(part, Map.of(DoorStateSchema.OPEN_RATIO, 1.0));
		assertEquals(Math.PI / 2, transform.rotationRadians(), 1.0e-9);
		assertEquals(new Vec3d(-500, 0, 0), transform.rotationAxisOrigin());
	}
}
