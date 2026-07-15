package dev.aperture.render.mesh;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.render.data.PartId;
import dev.aperture.render.data.RenderDocument;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeshBakeServiceTest {
	private final MeshBakeService bakeService = new MeshBakeService(new BoxMeshCompiler());

	@Test
	void incrementalBakeReusesUnchangedSections() {
		RenderDocument document = RenderDocument.forPreview(UUID.randomUUID());
		GeometryResult v1 = new GeometryResult(
			java.util.List.of(
				new GeometrySolid("frame", "frame", GeometryLayer.OPAQUE_FRAME, BoundingBox.fromSize(1000, 1200, 50)),
				new GeometrySolid("glazing", "glazing", GeometryLayer.TRANSLUCENT_GLASS, BoundingBox.fromSize(900, 1100, 10))
			),
			BoundingBox.fromSize(1000, 1200, 50),
			BoundingBox.fromSize(1000, 1200, 200)
		);

		var delta1 = document.updateFrom(v1);
		MeshAsset asset = bakeService.applyDelta(document, MeshAsset.empty(LODLevel.FULL), delta1, LODLevel.FULL);
		MeshHandle glazingHandle = asset.section(PartId.of("glazing")).orElseThrow().handle();

		GeometryResult v2 = new GeometryResult(
			java.util.List.of(
				new GeometrySolid("frame", "frame", GeometryLayer.OPAQUE_FRAME, BoundingBox.fromSize(1200, 1200, 50)),
				new GeometrySolid("glazing", "glazing", GeometryLayer.TRANSLUCENT_GLASS, BoundingBox.fromSize(900, 1100, 10))
			),
			BoundingBox.fromSize(1200, 1200, 50),
			BoundingBox.fromSize(1200, 1200, 200)
		);

		var delta2 = document.updateFrom(v2);
		MeshAsset patched = bakeService.applyDelta(document, asset, delta2, LODLevel.FULL);

		assertEquals(2, patched.partIds().size());
		assertTrue(delta2.unchanged().contains(PartId.of("glazing")));
		assertEquals(glazingHandle, patched.section(PartId.of("glazing")).orElseThrow().handle());
		assertTrue(patched.section(PartId.of("frame")).orElseThrow().handle().id() != asset.section(PartId.of("frame")).orElseThrow().handle().id());
	}

	@Test
	void boxCompilerProducesTwelveTriangles() {
		GeometrySolid solid = new GeometrySolid(
			"frame",
			"frame",
			GeometryLayer.OPAQUE_FRAME,
			BoundingBox.fromSize(100, 200, 30)
		);

		MeshSection section = new BoxMeshCompiler().compile(solid, LODLevel.FULL);

		assertEquals(12, section.triangleCount());
		assertEquals(PartId.of("frame"), section.partId());
	}
}
