package dev.aperture.editor.interaction;

import dev.aperture.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PerspectiveProjectorTest {
	private final PerspectiveProjector projector = new PerspectiveProjector();
	private final PerspectiveProjector.View view = new PerspectiveProjector.View(Vec3d.ZERO,
		new Vec3d(0, 0, 1), new Vec3d(0, 1, 0), 90, 1600, 900);

	@Test void forwardPointProjectsToViewportCenter() {
		assertEquals(new ScreenPoint(800, 450), projector.project(new Vec3d(0, 0, 10), view).orElseThrow());
	}

	@Test void pointsBehindCameraAreRejected() {
		assertTrue(projector.project(new Vec3d(0, 0, -10), view).isEmpty());
	}

	@Test void theSameWorldOffsetShrinksWithDistance() {
		double near = Math.abs(projector.project(new Vec3d(1, 0, 10), view).orElseThrow().x() - 800);
		double far = Math.abs(projector.project(new Vec3d(1, 0, 20), view).orElseThrow().x() - 800);
		assertEquals(near / 2.0, far, 1.0e-9);
	}
}
