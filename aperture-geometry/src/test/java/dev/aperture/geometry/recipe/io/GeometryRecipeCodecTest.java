package dev.aperture.geometry.recipe.io;

import dev.aperture.geometry.kernel.ProfileExtruder;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.profile.BuiltinProfiles;
import dev.aperture.geometry.recipe.EmitSolidOp;
import dev.aperture.geometry.recipe.GeometryRecipe;
import dev.aperture.geometry.recipe.GeometryRecipeBuilder;
import dev.aperture.geometry.recipe.GeometryRecipeExecutor;
import dev.aperture.geometry.recipe.SetCutVolumeOp;
import dev.aperture.geometry.recipe.shape.BoxRecipe;
import dev.aperture.geometry.recipe.shape.ExtrudeLinearRecipe;
import dev.aperture.geometry.recipe.shape.ShapeRecipes;
import dev.aperture.geometry.recipe.shape.SubtractBoxesRecipe;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Facing;
import dev.aperture.math.Transform3d;
import dev.aperture.math.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryRecipeCodecTest {
	@Test
	void roundTripsDeclarativeRecipe() {
		var builder = new GeometryRecipeBuilder();
		builder.emitSolid(
			"glazing",
			"glazing",
			GeometryLayer.TRANSLUCENT,
			ShapeRecipes.box(BoundingBox.fromSize(800, 1200, 10))
		);
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
		builder.setCutVolume(BoundingBox.fromSize(1000, 1200, 200));

		GeometryRecipe original = builder.build();
		GeometryRecipe restored = GeometryRecipeCodec.fromJson(GeometryRecipeCodec.toJson(original));

		assertEquals(original.ops().size(), restored.ops().size());
		EmitSolidOp glazing = (EmitSolidOp) restored.ops().getFirst();
		assertInstanceOf(BoxRecipe.class, glazing.shape());

		EmitSolidOp frame = (EmitSolidOp) restored.ops().get(1);
		assertInstanceOf(SubtractBoxesRecipe.class, frame.shape());
		assertInstanceOf(SetCutVolumeOp.class, restored.ops().getLast());

		assertEquals(
			GeometryRecipeExecutor.execute(original).solids().size(),
			GeometryRecipeExecutor.execute(restored).solids().size()
		);
	}

	@Test
	void roundTripsTransformWithRotation() {
		var builder = new GeometryRecipeBuilder();
		builder.emitSolid(
			"panel.0.glazing",
			"glazing",
			GeometryLayer.TRANSLUCENT,
			ShapeRecipes.box(BoundingBox.fromSize(400, 600, 8)),
			Transform3d.rotateAboutAxis(new Vec3d(0, 0, 0), ProfileExtruder.AXIS_Y, Math.toRadians(45))
		);
		builder.setCutVolume(BoundingBox.fromSize(1000, 1200, 200));

		GeometryRecipe restored = GeometryRecipeCodec.fromJson(GeometryRecipeCodec.toJson(builder.build()));
		EmitSolidOp emit = (EmitSolidOp) restored.ops().getFirst();
		assertTrue(emit.localTransform().hasRotation());
		assertEquals(Facing.NORTH, emit.localTransform().facing());
	}
}
