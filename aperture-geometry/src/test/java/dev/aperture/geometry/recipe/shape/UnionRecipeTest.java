package dev.aperture.geometry.recipe.shape;

import dev.aperture.geometry.kernel.ProfileExtruder;
import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.mesh.ShapeMesher;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.ops.BooleanOp;
import dev.aperture.geometry.profile.BuiltinProfiles;
import dev.aperture.geometry.recipe.EmitSolidOp;
import dev.aperture.geometry.recipe.GeometryRecipe;
import dev.aperture.geometry.recipe.GeometryRecipeBuilder;
import dev.aperture.geometry.recipe.ShapeRecipeEvaluator;
import dev.aperture.geometry.recipe.io.GeometryRecipeCodec;
import dev.aperture.geometry.shape.UnionShape;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class UnionRecipeTest {
	@Test
	void unionRecipeEvaluatesToUnionShape() {
		ShapeRecipe left = ShapeRecipes.box(BoundingBox.fromSize(100, 100, 50));
		ShapeRecipe right = ShapeRecipes.box(new BoundingBox(new Vec3d(100, 0, 0), new Vec3d(200, 100, 50)));
		ShapeRecipe recipe = ShapeRecipes.union(left, right);

		assertInstanceOf(UnionRecipe.class, recipe);
		assertInstanceOf(UnionShape.class, ShapeRecipeEvaluator.evaluate(recipe));
	}

	@Test
	void unionProfileRailsMatchesSeparateRailMeshes() {
		var profile = BuiltinProfiles.frameRect(50, 50);
		double width = 1000;
		double height = 1200;

		ShapeRecipe bottom = ShapeRecipes.extrudeLinear(
			profile,
			new Vec3d(0, 0, 0),
			new Vec3d(width, 0, 0),
			ProfileExtruder.AXIS_Y,
			ProfileExtruder.AXIS_Z
		);
		ShapeRecipe top = ShapeRecipes.extrudeLinear(
			profile,
			new Vec3d(0, height - 50, 0),
			new Vec3d(width, height - 50, 0),
			ProfileExtruder.AXIS_Y,
			ProfileExtruder.AXIS_Z
		);
		ShapeRecipe left = ShapeRecipes.extrudeLinear(
			profile,
			new Vec3d(0, 50, 0),
			new Vec3d(0, height - 50, 0),
			ProfileExtruder.AXIS_X,
			ProfileExtruder.AXIS_Z
		);
		ShapeRecipe right = ShapeRecipes.extrudeLinear(
			profile,
			new Vec3d(width - 50, 50, 0),
			new Vec3d(width - 50, height - 50, 0),
			ProfileExtruder.AXIS_X,
			ProfileExtruder.AXIS_Z
		);

		Mesh separate = ShapeMesher.meshLocal(BooleanOp.unionAll(java.util.List.of(
			ShapeRecipeEvaluator.evaluate(bottom),
			ShapeRecipeEvaluator.evaluate(top),
			ShapeRecipeEvaluator.evaluate(left),
			ShapeRecipeEvaluator.evaluate(right)
		)));
		Mesh merged = ShapeMesher.meshLocal(ShapeRecipeEvaluator.evaluate(ShapeRecipes.union(bottom, top, left, right)));

		assertEquals(separate.triangleCount(), merged.triangleCount());
		assertEquals(separate.vertexCount(), merged.vertexCount());
	}

	@Test
	void unionRecipeRoundTripsThroughJsonCodec() {
		ShapeRecipe recipe = ShapeRecipes.union(
			ShapeRecipes.box(BoundingBox.fromSize(100, 100, 50)),
			ShapeRecipes.extrudeLinear(
				BuiltinProfiles.frameRect(50, 50),
				new Vec3d(0, 0, 0),
				new Vec3d(500, 0, 0),
				ProfileExtruder.AXIS_Y,
				ProfileExtruder.AXIS_Z
			)
		);

		var builder = new GeometryRecipeBuilder();
		builder.emitSolid("frame.assembly", "frame", GeometryLayer.OPAQUE, recipe);
		builder.setCutVolume(BoundingBox.fromSize(1000, 1200, 200));

		GeometryRecipe restored = GeometryRecipeCodec.fromJson(GeometryRecipeCodec.toJson(builder.build()));
		EmitSolidOp emit = (EmitSolidOp) restored.ops().getFirst();
		assertInstanceOf(UnionRecipe.class, emit.shape());
		assertEquals(2, ((UnionRecipe) emit.shape()).operands().size());
	}

	@Test
	void converterRecoversUnionFromEvaluatedShape() {
		ShapeRecipe original = ShapeRecipes.union(
			ShapeRecipes.box(BoundingBox.fromSize(50, 50, 50)),
			ShapeRecipes.box(BoundingBox.fromSize(60, 60, 60))
		);
		ShapeRecipe converted = ShapeRecipeConverter.fromSolid(ShapeRecipeEvaluator.evaluate(original));
		assertInstanceOf(UnionRecipe.class, converted);
		assertEquals(2, ((UnionRecipe) converted).operands().size());
	}
}
