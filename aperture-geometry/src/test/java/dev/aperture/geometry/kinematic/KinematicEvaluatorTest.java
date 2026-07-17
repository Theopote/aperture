package dev.aperture.geometry.kinematic;

import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KinematicEvaluatorTest {
	@Test
	void evaluatesRotationDirectlyFromStateDriver() {
		KinematicPart panel = new KinematicPart(
			new ComponentPath("panel.main"), Transform3d.identity(), null,
			new Pivot(new Vec3d(-450, 0, 0)),
			new MotionDefinition(MovementType.ROTATE, new Vec3d(0, 1, 0), 0, Math.PI / 2, "openRatio"));

		Transform3d halfway = KinematicEvaluator.evaluate(panel, Map.of("openRatio", 0.5));
		assertEquals(Math.PI / 4, halfway.rotationRadians(), 1.0e-9);
		assertEquals(new Vec3d(-450, 0, 0), halfway.rotationAxisOrigin());
	}

	@Test
	void evaluatesTranslationAndClampsDriver() {
		KinematicPart sash = new KinematicPart(
			new ComponentPath("panel.sliding"), Transform3d.identity(), null,
			new Pivot(Vec3d.ZERO),
			new MotionDefinition(MovementType.TRANSLATE, new Vec3d(1, 0, 0), 0, 800, "openRatio"));

		assertEquals(new Vec3d(800, 0, 0),
			KinematicEvaluator.evaluate(sash, Map.of("openRatio", 2.0)).origin());
		assertThrows(IllegalArgumentException.class, () -> KinematicEvaluator.evaluate(sash, Map.of()));
	}
}
