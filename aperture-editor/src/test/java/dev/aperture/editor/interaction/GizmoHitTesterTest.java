package dev.aperture.editor.interaction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GizmoHitTesterTest {
	private static ScreenSpaceHandle handle(String id, double x, double radius, boolean enabled) {
		return new ScreenSpaceHandle(id, new ScreenPoint(x, 50), 9, radius, 14, enabled,
			ScreenSpaceHandle.OcclusionPolicy.IGNORE_SCENE_DEPTH,
			ScreenSpaceHandle.DisplayPolicy.ALWAYS_ON_TOP);
	}

	@Test
	void radiusIsMeasuredInPixelsAndBoundaryIsInclusive() {
		var tester = new GizmoHitTester();
		assertTrue(tester.hit(new ScreenPoint(112, 50), List.of(handle("width", 100, 12, true))).isPresent());
		assertTrue(tester.hit(new ScreenPoint(112.01, 50), List.of(handle("width", 100, 12, true))).isEmpty());
	}

	@Test
	void nearestEnabledHandleWins() {
		var tester = new GizmoHitTester();
		var hit = tester.hit(new ScreenPoint(104, 50), List.of(
			handle("disabled", 104, 12, false), handle("far", 96, 12, true), handle("near", 105, 12, true)));
		assertEquals("near", hit.orElseThrow().id());
	}
}
