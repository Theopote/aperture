package dev.aperture.opening.geometry.pipeline.frame;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.geometry.mesh.ShapeMesher;
import dev.aperture.geometry.recipe.GeometryRecipeBuilder;
import dev.aperture.geometry.recipe.GeometryRecipeExecutor;
import dev.aperture.geometry.recipe.shape.UnionRecipe;
import dev.aperture.opening.geometry.generator.GenerationTestSupport;
import dev.aperture.opening.geometry.pipeline.OpeningLayout;
import dev.aperture.opening.geometry.pipeline.OpeningPipelineContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FrameRailUnionTest {
	@Test
	void unionFrameShapeMatchesFourSeparateRails() {
		var context = GenerationTestSupport.context(BuiltinOpeningTypes.fixedWindow(), ParameterSet.empty());
		OpeningPipelineContext pipelineContext = OpeningPipelineContext.from(context);
		var profile = pipelineContext.resolvedProfiles().frame().curve();
		OpeningLayout layout = pipelineContext.layout();

		var separate = new GeometryRecipeBuilder();
		FrameRailBuilder.emitMiteredRail(separate, "frame.bottom", profile,
			new dev.aperture.math.Vec3d(0, 0, 0), new dev.aperture.math.Vec3d(layout.width(), 0, 0),
			FrameRailBuilder.axisY(), FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.BOTTOM_LEFT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.BOTTOM_RIGHT));
		FrameRailBuilder.emitMiteredRail(separate, "frame.top", profile,
			new dev.aperture.math.Vec3d(0, layout.height() - layout.frameFace(), 0),
			new dev.aperture.math.Vec3d(layout.width(), layout.height() - layout.frameFace(), 0),
			FrameRailBuilder.axisY(), FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_LEFT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_RIGHT));
		FrameRailBuilder.emitMiteredRail(separate, "frame.left", profile,
			new dev.aperture.math.Vec3d(0, layout.frameFace(), 0),
			new dev.aperture.math.Vec3d(0, layout.height() - layout.frameFace(), 0),
			FrameRailBuilder.axisX(), FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.BOTTOM_LEFT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_LEFT));
		FrameRailBuilder.emitMiteredRail(separate, "frame.right", profile,
			new dev.aperture.math.Vec3d(layout.width() - layout.frameFace(), layout.frameFace(), 0),
			new dev.aperture.math.Vec3d(layout.width() - layout.frameFace(), layout.height() - layout.frameFace(), 0),
			FrameRailBuilder.axisX(), FrameRailBuilder.axisZ(),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.BOTTOM_RIGHT),
			FrameRailBuilder.corner(layout, FrameRailBuilder.Corner.TOP_RIGHT));
		separate.setCutVolume(dev.aperture.math.BoundingBox.fromSize(layout.width(), layout.height(), 200));

		var unified = new GeometryRecipeBuilder();
		FrameRailBuilder.emitUnionFrame(unified, "frame.assembly", profile, layout);
		unified.setCutVolume(dev.aperture.math.BoundingBox.fromSize(layout.width(), layout.height(), 200));

		var separateGeometry = GeometryRecipeExecutor.execute(separate.build());
		var unifiedGeometry = GeometryRecipeExecutor.execute(unified.build());

		int separateTriangles = separateGeometry.solids().stream()
			.mapToInt(solid -> ShapeMesher.mesh(solid.shape(), solid.localTransform()).triangleCount())
			.sum();
		int unifiedTriangles = ShapeMesher.mesh(
			unifiedGeometry.solids().getFirst().shape(),
			unifiedGeometry.solids().getFirst().localTransform()
		).triangleCount();

		assertEquals(separateTriangles, unifiedTriangles);
		assertInstanceOf(UnionRecipe.class,
			((dev.aperture.geometry.recipe.