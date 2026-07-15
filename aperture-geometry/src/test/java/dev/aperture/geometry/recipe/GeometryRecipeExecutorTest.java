package dev.aperture.geometry.recipe;

import dev.aperture.geometry.kernel.ProfileExtruder;
import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.mesh.ShapeMesher;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.profile.BuiltinProfiles;
import dev.aperture.geometry.recipe.shape.ExtrudeLinearRecipe;
import dev.aperture.geometry.recipe.shape.ShapeRecipes;
import dev.aperture.geometry.recipe.shape.SubtractBoxesRecipe;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryRecipeExecutorTest {
	@Test
	void extrudeLinearRecipeMatchesProfileExtruder() {
		var profile = BuiltinProfiles.frameRect(50, 50);
		var pathStart = new Vec3d(0, 0, 0);
		var pathEnd = new Vec3d(1000, 0, 0);
		var profileU = ProfileExtruder.AXIS_Y;
		var profileV = ProfileExtruder.AXIS_Z;
		var corner = new BoundingBox(Vec3d.ZERO, new Vec3d(50, 50, 50));

		GeometrySolid direct = ProfileExtruder.linear(
			"frame.bottom",
			"frame",
			GeometryLayer.OPAQUE,
			profile,
			pathStart,
			pathEnd,
			profileU,
			profileV,
			corner
		);

		var builder = new GeometryRecipeBuilder();
		builder.emitSolid(
			"frame.bottom",
			"frame",
			GeometryLayer.OPAQUE,
			ShapeRecipes.extrudeLinear(profile, pathStart, pathEnd, profileU, profileV, corner)
		);
		builder.setCutVolume(BoundingBox.fromSize(1000, 1000, 200));
		GeometrySolid fromRecipe = builder.execute().solids().getFirst();

		assertEquals(direct.componentPath(), fromRecipe.componentPath());
		assertEquals(direct.bounds().width(), fromRecipe.bounds().width(), 0.01);
		assertEquals(
			ShapeMesher.meshLocal(direct.shape()).triangleCount(),
			ShapeMesher.meshLocal(fromRecipe.shape()).triangleCount()
		);
	}

	@Test
	void recipePreservesDeclarativeSubtractOps() {
		var builder = new GeometryRecipeBuilder();
		builder.emitSolid(
			"frame.bottom",
			"frame",
			GeometryLayer.OPAQUE,
			ShapeRecipes.extrudeLinear(
				BuiltinProfiles.frameRect(50, 50),
				new Vec3d(0, 0, 0),
				new Vec3d(1000, 0, 0),
				ProfileExtruder.AXIS_Y,
				ProfileExtruder.AXIS_Z,
				new BoundingBox(Vec3d.ZERO, new Vec3d(50, 50, 50))
			)
		);
		builder.setCutVolume(BoundingBox.fromSize(1000, 1000, 200));

		GeometryRecipe recipe = builder.build();
		EmitSolidOp emit = (EmitSolidOp) recipe.ops().getFirst();
		assertInstanceOf(SubtractBoxesRecipe.class, emit.shape());
		assertInstanceOf(ExtrudeLinearRecipe.class, ((SubtractBoxesRecipe) emit.shape()).base());
	}

	@Test
	void subtractBoxesRecipeTrimsCornerTriangles() {
		var profile = BuiltinProfiles.frameRect(50, 50);
		var corner = new BoundingBox(Vec3d.ZERO, new Vec3d(50, 50, 50));
		var shapeRecipe = ShapeRecipes.extrudeLinear(
			profile,
			new Vec3d(0, 0, 0),
			new Vec3d(1000, 0, 0),
			ProfileExtruder.AXIS_Y,
			ProfileExtruder.AXIS_Z,
			corner
		);

		Mesh full = ShapeMesher.meshLocal(ShapeRecipeEvaluator.evaluate(
			ShapeRecipes.extrudeLinear(profile, new Vec3d(0, 0, 0), new Vec3d(1000, 0, 0), ProfileExtruder.AXIS_Y, ProfileExtruder.AXIS_Z)
		));
		Mesh trimmed = ShapeMesher.meshLocal(ShapeRecipeEvaluator.evaluate(shapeRecipe));

		assertTrue(countTrianglesInside(trimmed, corner) < countTrianglesInside(full, corner));
	}

	private static int countTrianglesInside(Mesh mesh, BoundingBox box) {
		int count = 0;
		for (int triangle = 0; triangle < mesh.triangleCount(); triangle++) {
			int index = triangle * 3;
			Vec3d a = readPosition(mesh, mesh.indices()[index]);
			Vec3d b = readPosition(mesh, mesh.indices()[index + 1]);
			Vec3d c = readPosition(mesh, mesh.indices()[index + 2]);
			Vec3d centroid = new Vec3d(
				(a.x() + b.x() + c.x()) / 3.0,
				(a.y() + b.y() + c.y()) / 3.0,
				(a.z() + b.z() + c.z()) / 3.0
			);
			if (isInside(centroid, box)) {
				count++;
			}
		}
		return count;
	}

	private static boolean isInside(Vec3d point, BoundingBox box) {
		return point.x() >= box.min().x() && point.x() <= box.max().x()
			&& point.y() >= box.min().y() && point.y() <= box.max().y()
			&& point.z() >= box.min().z() && point.z() <= box.max().z();
	}

	private static Vec3d readPosition(Mesh mesh, int vertexIndex) {
		int offset = vertexIndex * Mesh.FLOATS_PER_VERTEX;
		float[] vertices = mesh.vertices();
		return new Vec3d(vertices[offset], vertices[offset + 1], vertices[offset + 2]);
	}
}
