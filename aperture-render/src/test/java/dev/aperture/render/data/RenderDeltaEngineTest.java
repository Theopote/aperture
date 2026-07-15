package dev.aperture.render.data;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.opening.geometry.generator.RectangularWindowGenerator;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.PartId;
import dev.aperture.geometry.profile.ProfileCatalogLoader;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderDeltaEngineTest {
	private static final RectangularWindowGenerator GENERATOR = new RectangularWindowGenerator();
	private static final ProfileCatalogLoader PROFILE_LOADER = new ProfileCatalogLoader();

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
	void widthChangeMarksWidthDependentPartsChanged() {
		GeometryResult before = window(1200, 1500, 0);
		GeometryResult after = window(1400, 1500, 0);

		RenderDelta delta = RenderDeltaEngine.compute(before, after);

		assertTrue(delta.added().isEmpty());
		assertTrue(delta.removed().isEmpty());
		assertEquals(
			Set.of(
				PartId.of("frame.bottom"),
				PartId.of("frame.top"),
				PartId.of("frame.right"),
				PartId.of("glazing")
			),
			delta.changed()
		);
		assertEquals(Set.of(PartId.of("frame.left")), delta.unchanged());
	}

	@Test
	void mullionCountChangeAddsMullionsAndUpdatesExistingPositions() {
		GeometryResult before = window(1200, 1500, 1);
		GeometryResult after = window(1200, 1500, 2);

		RenderDelta delta = RenderDeltaEngine.compute(before, after);

		assertEquals(Set.of(PartId.of("mullions.mullion.2")), delta.added());
		assertTrue(delta.removed().isEmpty());
		assertEquals(Set.of(PartId.of("mullions.mullion.1")), delta.changed());
		assertTrue(delta.unchanged().contains(PartId.of("frame.bottom")));
		assertTrue(delta.unchanged().contains(PartId.of("glazing")));
	}

	private static GeometryResult window(double width, double height, int mullions) {
		var definition = BuiltinOpeningTypes.fixedWindow();
		GenerationContext context = new GenerationContext(
			definition,
			definition.resolveParameters( ParameterSet.builder()
				.put("width", ParameterValue.length(width))
				.put("height", ParameterValue.length(height))
				.put("mullions", ParameterValue.count(mullions))
				.build()),
			PROFILE_LOADER.loadClasspathCatalog()
		);
		return GENERATOR.generate(context).geometry();
	}
}
