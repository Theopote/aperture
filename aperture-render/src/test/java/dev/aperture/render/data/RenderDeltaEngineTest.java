package dev.aperture.render.data;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderDeltaEngineTest {
	@Test
	void firstSnapshotIsAllAdded() {
		GeometryResult next = window(1200, 1500, 0);
		RenderDelta delta = RenderDeltaEngine.compute(null, next);

		assertEquals(Set.of(PartId.of("frame"), PartId.of("glazing")), delta.added());
		assertTrue(delta.removed().isEmpty());
		assertTrue(delta.changed().isEmpty());
		assertTrue(delta.unchanged().isEmpty());
	}

	@Test
	void widthChangeMarksFrameAndGlazingChanged() {
		GeometryResult before = window(1200, 1500, 0);
		GeometryResult after = window(1400, 1500, 0);

		RenderDelta delta = RenderDeltaEngine.compute(before, after);

		assertTrue(delta.added().isEmpty());
		assertTrue(delta.removed().isEmpty());
		assertEquals(Set.of(PartId.of("frame"), PartId.of("glazing")), delta.changed());
		assertTrue(delta.unchanged().isEmpty());
	}

	@Test
	void mullionCountChangeAddsAndRemovesMullions() {
		GeometryResult before = window(1200, 1500, 1);
		GeometryResult after = window(1200, 1500, 2);

		RenderDelta delta = RenderDeltaEngine.compute(before, after);

		assertEquals(Set.of(PartId.of("frame.mullion.2")), delta.added());
		assertTrue(delta.removed().isEmpty());
		assertTrue(delta.changed().contains(PartId.of("frame.mullion.1")));
		assertTrue(delta.unchanged().contains(PartId.of("frame")));
		assertTrue(delta.unchanged().contains(PartId.of("glazing")));
	}

	private static GeometryResult window(double width, double height, int mullions) {
		double frameWidth = 50;
		List<GeometrySolid> solids = new java.util.ArrayList<>();
		solids.add(new GeometrySolid("frame", "frame", GeometryLayer.OPAQUE_FRAME, BoundingBox.fromSize(width, height, frameWidth)));

		double innerWidth = width - frameWidth * 2;
		double innerHeight = height - frameWidth * 2;
		solids.add(new GeometrySolid(
			"glazing",
			"glazing",
			GeometryLayer.TRANSLUCENT_GLASS,
			new BoundingBox(new Vec3d(frameWidth, frameWidth, 0), new Vec3d(frameWidth + innerWidth, frameWidth + innerHeight, 10))
		));

		for (int i = 1; i <= mullions; i++) {
			double t = (double) i / (mullions + 1);
			double x = frameWidth + innerWidth * t;
			solids.add(new GeometrySolid(
				"frame.mullion." + i,
				"frame",
				GeometryLayer.OPAQUE_FRAME,
				new BoundingBox(
					new Vec3d(x - frameWidth / 2.0, frameWidth, 0),
					new Vec3d(x + frameWidth / 2.0, frameWidth + innerHeight, frameWidth)
				)
			));
		}

		return new GeometryResult(solids, BoundingBox.fromSize(width, height, frameWidth), BoundingBox.fromSize(width, height, 200));
	}
}
