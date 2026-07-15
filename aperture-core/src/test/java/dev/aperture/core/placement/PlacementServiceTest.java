package dev.aperture.core.placement;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.geometry.primitives.BoundingBox;
import dev.aperture.geometry.primitives.Transform3d;
import dev.aperture.core.instance.HostBinding;
import dev.aperture.core.instance.InMemoryOpeningInstanceStore;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacementServiceTest {
	private OpeningTypeRegistry openingTypes;
	private InMemoryOpeningInstanceStore instances;
	private PlacementService placement;

	@BeforeEach
	void setUp() {
		openingTypes = new OpeningTypeRegistry();
		openingTypes.register(BuiltinOpeningTypes.fixedWindow());
		instances = new InMemoryOpeningInstanceStore();
		placement = new PlacementService(openingTypes, instances);
	}

	@Test
	void previewAcceptsValidPlacementWithinHost() {
		PlacementContext context = PlacementContext.of(
			HostBinding.wall("section:0,0,0"),
			BoundingBox.fromSize(3000, 3000, 500)
		);

		PlacementSession session = placement.preview(
			BuiltinOpeningTypes.FIXED_WINDOW_ID,
			ParameterSet.empty(),
			Transform3d.at(100, 100, 0, dev.aperture.geometry.primitives.Facing.NORTH),
			HostBinding.wall("section:0,0,0"),
			context
		);

		assertTrue(session.isValid());
	}

	@Test
	void previewRejectsOpeningOutsideHostBounds() {
		PlacementContext context = PlacementContext.of(
			HostBinding.wall("section:0,0,0"),
			BoundingBox.fromSize(1000, 1000, 500)
		);

		PlacementSession session = placement.preview(
			BuiltinOpeningTypes.FIXED_WINDOW_ID,
			ParameterSet.empty(),
			Transform3d.at(500, 500, 0, dev.aperture.geometry.primitives.Facing.NORTH),
			HostBinding.wall("section:0,0,0"),
			context
		);

		assertFalse(session.isValid());
		assertTrue(session.validationReport().issues().stream()
			.anyMatch(issue -> issue.code().equals("placement.fits_within_host")));
	}

	@Test
	void previewRejectsOverlapWithExistingInstance() {
		OpeningInstance existing = OpeningInstance.builder(BuiltinOpeningTypes.FIXED_WINDOW_ID)
			.instanceId(UUID.randomUUID())
			.parameters(ParameterSet.builder()
				.put("width", ParameterValue.length(1200))
				.put("height", ParameterValue.length(1500))
				.put("mullions", ParameterValue.count(0))
				.put("frame_width", ParameterValue.length(50))
				.build())
			.transform(Transform3d.at(100, 100, 0, dev.aperture.core.geometry.Facing.NORTH))
			.host(HostBinding.wall("section:0,0,0"))
			.build();
		instances.put(existing);

		PlacementContext context = new PlacementContext(
			HostBinding.wall("section:0,0,0"),
			BoundingBox.fromSize(3000, 3000, 500),
			List.of(existing)
		);

		PlacementSession session = placement.preview(
			BuiltinOpeningTypes.FIXED_WINDOW_ID,
			ParameterSet.empty(),
			Transform3d.at(200, 200, 0, dev.aperture.geometry.primitives.Facing.NORTH),
			HostBinding.wall("section:0,0,0"),
			context
		);

		assertFalse(session.isValid());
		assertTrue(session.validationReport().issues().stream()
			.anyMatch(issue -> issue.code().equals("placement.no_overlap")));
	}

	@Test
	void commitStoresValidPreview() {
		PlacementContext context = PlacementContext.of(
			HostBinding.wall("section:0,0,0"),
			BoundingBox.fromSize(3000, 3000, 500)
		);

		PlacementSession session = placement.preview(
			BuiltinOpeningTypes.FIXED_WINDOW_ID,
			ParameterSet.empty(),
			Transform3d.at(0, 0, 0, dev.aperture.geometry.primitives.Facing.NORTH),
			HostBinding.wall("section:0,0,0"),
			context
		);

		OpeningInstance committed = placement.commit(session);

		assertEquals(committed, instances.findById(committed.instanceId()).orElseThrow());
	}

	@Test
	void commitRejectsInvalidPreview() {
		PlacementContext context = PlacementContext.of(
			HostBinding.wall("section:0,0,0"),
			BoundingBox.fromSize(500, 500, 500)
		);

		PlacementSession session = placement.preview(
			BuiltinOpeningTypes.FIXED_WINDOW_ID,
			ParameterSet.empty(),
			Transform3d.at(0, 0, 0, dev.aperture.geometry.primitives.Facing.NORTH),
			HostBinding.wall("section:0,0,0"),
			context
		);

		assertThrows(IllegalStateException.class, () -> placement.commit(session));
	}
}
