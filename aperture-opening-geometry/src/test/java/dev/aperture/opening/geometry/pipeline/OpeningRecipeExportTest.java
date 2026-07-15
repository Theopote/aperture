package dev.aperture.opening.geometry.pipeline;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.geometry.export.GeometryExport;
import dev.aperture.geometry.export.gltf.GltfExporter;
import dev.aperture.geometry.recipe.EmitSolidOp;
import dev.aperture.geometry.recipe.GeometryRecipe;
import dev.aperture.geometry.recipe.GeometryRecipeExecutor;
import dev.aperture.geometry.recipe.io.GeometryRecipeCodec;
import dev.aperture.geometry.recipe.shape.BoxRecipe;
import dev.aperture.geometry.recipe.shape.ExtrudeLinearRecipe;
import dev.aperture.geometry.recipe.shape.SubtractBoxesRecipe;
import dev.aperture.opening.geometry.generator.GenerationTestSupport;
import dev.aperture.opening.pipeline.OpeningGenerationPipeline;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpeningRecipeExportTest {
	private static final OpeningGenerationPipeline PIPELINE = OpeningGenerationPipeline.standard();

	@Test
	void fixedWindowRecipeUsesDeclarativeShapeOps() {
		var context = GenerationTestSupport.context(BuiltinOpeningTypes.fixedWindow(), ParameterSet.empty());
		GeometryRecipe recipe = PIPELINE.compileRecipe(context);

		assertFalse(recipe.ops().isEmpty());
		assertTrue(recipe.ops().stream().anyMatch(op -> op instanceof EmitSolidOp emit
			&& emit.componentPath().equals("frame.bottom")
			&& emit.shape() instanceof SubtractBoxesRecipe));
		assertTrue(recipe.ops().stream().anyMatch(op -> op instanceof EmitSolidOp emit
			&& emit.componentPath().equals("glazing")
			&& emit.shape() instanceof BoxRecipe));
	}

	@Test
	void casementPanelRecipeUsesExtrudeLinearOps() {
		var context = GenerationTestSupport.context(BuiltinOpeningTypes.casementWindow(), ParameterSet.builder()
			.put("open_angle", ParameterValue.angle(30))
			.build());
		GeometryRecipe recipe = PIPELINE.compileRecipe(context);

		assertTrue(recipe.ops().stream().anyMatch(op -> op instanceof EmitSolidOp emit
			&& emit.componentPath().equals("panel.bottom")
			&& emit.shape() instanceof ExtrudeLinearRecipe));
	}

	@Test
	void doorRecipeUsesDeclarativeHardwareAndSill() {
		var context = GenerationTestSupport.context(BuiltinOpeningTypes.door(), ParameterSet.empty());
		GeometryRecipe recipe = PIPELINE.compileRecipe(context);

		assertTrue(recipe.ops().stream().anyMatch(op -> op instanceof EmitSolidOp emit
			&& emit.componentPath().equals("threshold.main")
			&& emit.shape() instanceof BoxRecipe));
		assertTrue(recipe.ops().stream().anyMatch(op -> op instanceof EmitSolidOp emit
			&& emit.componentPath().equals("handle.main")
			&& emit.shape() instanceof BoxRecipe));
	}

	@Test
	void recipeJsonRoundTripAndGltfExport() {
		var context = GenerationTestSupport.context(BuiltinOpeningTypes.fixedWindow(), ParameterSet.empty());
		CompiledPipeline compiled = PIPELINE.compile(context);

		GeometryRecipe restored = GeometryRecipeCodec.fromJson(GeometryRecipeCodec.toJson(compiled.recipe()));
		assertEquals(
			compiled.geometry().solids().size(),
			GeometryRecipeExecutor.execute(restored).solids().size()
		);

		JsonObject root = JsonParser.parseString(GeometryExport.toGltf(compiled.recipe())).getAsJsonObject();
		assertEquals(GltfExporter.GENERATOR, root.getAsJsonObject("asset").get("generator").getAsString());
		assertFalse(root.getAsJsonArray("meshes").isEmpty());
	}
}
