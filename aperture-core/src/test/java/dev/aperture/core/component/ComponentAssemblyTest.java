package dev.aperture.core.component;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentAssemblyTest {
	@Test
	void presetsExposeExpectedKindsForDoorAndWindow() {
		ComponentAssembly window = ComponentAssemblyPresets.fixedWindow(
			"aperture:frame_l_50x80",
			"aperture:single_glazed"
		);
		assertTrue(window.has(ComponentKind.FRAME));
		assertTrue(window.has(ComponentKind.GLASS));
		assertTrue(window.has(ComponentKind.DIVIDER));
		assertFalse(window.has(ComponentKind.PANEL));

		ComponentAssembly door = ComponentAssemblyPresets.door(
			"aperture:frame_standard_50",
			"aperture:frame_standard_50",
			"aperture:single_glazed",
			"left"
		);
		assertTrue(door.has(ComponentKind.PANEL));
		assertTrue(door.has(ComponentKind.HARDWARE));
		assertTrue(door.has(ComponentKind.SILL));
	}

	@Test
	@SuppressWarnings("deprecation")
	void convertsLegacyMapIntoTypedComponents() {
		ComponentAssembly assembly = ComponentAssembly.fromLegacyMap(Map.of(
			"frame", Map.of("profile", "aperture:frame_l_50x80"),
			"panel", Map.of("profile", "aperture:frame_standard_50", "hinge", "right"),
			"glazing", Map.of("system", "aperture:single_glazed")
		));

		assertEquals("aperture:frame_l_50x80", assembly.frame().orElseThrow().profileId());
		assertEquals("right", assembly.panel().orElseThrow().hinge());
		assertEquals("aperture:single_glazed", assembly.glass().orElseThrow().systemId());
	}

	@Test
	void curtainWallPresetComposesGridComponents() {
		ComponentAssembly curtainWall = ComponentAssemblyPresets.curtainWall(
			"aperture:frame_l_50x80",
			"aperture:single_glazed"
		);

		assertTrue(curtainWall.has(ComponentKind.FRAME));
		assertEquals(2, curtainWall.ofKind(ComponentKind.DIVIDER).size());
		assertTrue(curtainWall.has(ComponentKind.HEADER));
		assertTrue(curtainWall.has(ComponentKind.SILL));
	}
}
