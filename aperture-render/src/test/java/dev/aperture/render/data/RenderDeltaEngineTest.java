package dev.aperture.render.data;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.geometry.generator.RectangularWindowGenerator;
import dev.aperture.geometry.model.GeometryResult;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderDeltaEngineTest {
	private static final RectangularWindowGenerator GENERATOR = new RectangularWindowGenerator();

	@Test
	void firstSnapshotIsAllAdded() {
		GeometryResult next = window(1200, 1500, 0);
		RenderDelta delta = RenderDeltaEngine.compute(null, next);

		assertEquals(
			Set.of(
				PartId.of("frame.bottom"),
				PartId.of("frame.top"),
				PartId.of("frame.left"),
				PartId.of("frame.right"),
				PartId.of("glazing")
			),
			delta.added()
		);
		assertTrue(delta.removed().isEmpty());
		assertTrue(delta.changed().isEmpty());
		assertTrue(delta.unchanged().isEmpty());
	}

	@Test
	void widthChangeMarksAllPartsChanged() {
		GeometryResult before = window(1200, 1500, 0);
		GeometryResult after = window(1400, 1500, 0);

		RenderDelta delta = RenderDeltaEngine.compute(before, after);

		assertTrue(delta.added().isEmpty());
		assertTrue(delta.removed().isEmpty());
		assertEquals(5, delta.changed().size());
		assertTrue(delta.unchanged().isEmpty());
	}

	@Test
	void mullionCountChangeAddsAndRemovesMullions() {
		GeometryResult before = window(1200, 1500, 1);
		GeometryResult after = window(1200, 1500, 2);

		RenderDelta delta = RenderDeltaEngine.compute(before, after);

		assertEquals(Set.of(PartId.of("frame.mullion.2")), delta.added());
		assertTrue(delta.removed().isEmpty());
		assertTrue(delta.unchanged().contains(PartId.of("frame.mullion.1")));
		assertTrue(delta.unchanged().contains(PartId.of("frame.bottom")));
		assertTrue(delta.unchanged().contains(PartId.of("glazing")));
	}

	private static GeometryResult window(double width, double height, int mullions) {
		var definition = BuiltinOpeningTypes.fixedWindow();
		ParameterSet parameters = ParameterSet.mergeDefaults(definition.parameters(), ParameterSet.builder()
			.put("width", ParameterValue.length(width))
			.put("height", ParameterValue.length(height))
			.put("mullions", ParameterValue.count(mullions))
			.build());
		return GENERATOR.generate(definition, parameters);
	}
}
