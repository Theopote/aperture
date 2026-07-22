package dev.aperture.editor.interaction;

import dev.aperture.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EditorInputFrameTest {
	private static final WorldRay RAY = new WorldRay(Vec3d.ZERO, new Vec3d(0, 0, 1));

	@Test void uiMouseCaptureSuppressesWorldInteraction() {
		assertFalse(frame(true, true, RAY).worldInteractionAllowed());
	}

	@Test void pointerMustBeInsideViewportAndRayMustExist() {
		assertFalse(frame(false, false, RAY).worldInteractionAllowed());
		assertFalse(frame(false, true, null).worldInteractionAllowed());
		assertTrue(frame(false, true, RAY).worldInteractionAllowed());
	}

	private static EditorInputFrame frame(boolean capture, boolean inside, WorldRay ray) {
		return new EditorInputFrame(false, false, false, false, false, false, false,
			capture, false, inside, new ScreenPoint(10, 10), ray);
	}
}
