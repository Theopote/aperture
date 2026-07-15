package dev.aperture.render.mesh;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.math.BoundingBox;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.opening.geometry.generator.RectangularWindowGenerator;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.geometry.profile.ProfileCatalogLoader;
import dev.aperture.geometry.model.PartId;
import dev.aperture.render.data.RenderDocument;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeshBakeServiceTest {
	private final MeshBakeService bakeService = new MeshBakeService(new SolidShapeMeshCompiler());
	private static final ProfileCatalogLoader PROFILE_LOADER = new ProfileCatalogLoader();

	@Test
	void incrementalBakeReusesUnchangedSections() {
		RenderDocument document = RenderDocument.forPreview(UUID.randomUUID());
		GeometryResult v1 = new GeometryResult(
			java.util.List.of(
				GeometrySolid.box("frame", "frame", GeometryLayer.OPAQUE, BoundingBox.fromSize(1000, 1200, 50)),
				GeometrySolid.box("glazing", "glazing", GeometryLayer.TRANSLUCENT, BoundingBox.fromSize(900, 1100, 10))
			),
			BoundingBox.fromSize(1000, 1200, 50),
			BoundingBox.fromSize(1000, 1200, 200)
		);

		var delta1 = document.updateFrom(v1);
		MeshAsset asset = bakeService.applyDelta(document, MeshAsset.empty(LODLevel.FULL), delta1, LODLevel.FULL);
		MeshHandle glazingHandle = asset.section(PartId.of("glazing")).orElseThrow().handle();

		GeometryResult v2 = new GeometryResult(
			java.util.List.of(
				GeometrySolid.box("frame", "frame", GeometryLayer.OPAQUE, BoundingBox.fromSize(1200, 1200, 50)),
				GeometrySolid.box("glazing", "glazing", GeometryLayer.TRANSLUCENT, BoundingBox.fromSize(900, 1100, 10))
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
	void solidShapeCompilerProducesTwelveTrianglesForBox() {
		GeometrySolid solid = GeometrySolid.box(
			"frame",
			"frame",
			GeometryLayer.OPAQUE,
			BoundingBox.fromSize(100, 200, 30)
		);

		MeshSection section = new SolidShapeMeshCompiler().compile(solid, LODLevel.FULL);

		assertEquals(12, section.triangleCount());
		assertEquals(PartId.of("frame"), section.partId());
	}

	@Test
	void assemblyDeltaReusesUnchangedSections() {
		RenderDocument document = RenderDocument.forPreview(UUID.randomUUID());
		PipelineResult before = windowPipeline(1200, 1500, 1);
		var delta1 = document.updateFrom(before.geometry());
		MeshAsset asset = bakeService.applyDeltaFromAssembly(document, MeshAsset.empty(LODLevel.FULL), delta1, before, LODLevel.FULL);
		MeshHandle glazingHandle = asset.section(PartId.of("glazing")).orElseThrow().handle();

		PipelineResult after = windowPipeline(1200, 1500, 2);
		var delta2 = document.updateFrom(after.geometry());
		MeshAsset patched = bakeService.applyDeltaFromAssembly(document, asset, delta2, after, LODLevel.FULL);

		assertEquals(7, patched.partIds().size());
		assertTrue(delta2.unchanged().contains(PartId.of("glazing")));
		assertEquals(glazingHandle, patched.section(PartId.of("glazing")).orElseThrow().handle());
		assertTrue(patched.section(PartId.of("mullions.mullion.2")).isPresent());
	}

	private static PipelineResult windowPipeline(double width, double height, int mullions) {
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
		return new RectangularWindowGenerator().generate(context);
	}
}
