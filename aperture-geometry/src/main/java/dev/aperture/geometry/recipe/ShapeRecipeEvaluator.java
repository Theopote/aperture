package dev.aperture.geometry.recipe;

import dev.aperture.geometry.ops.BooleanOp;
import dev.aperture.geometry.ops.ExtrudeOp;
import dev.aperture.geometry.recipe.shape.BoxRecipe;
import dev.aperture.geometry.recipe.shape.ExtrudeLinearRecipe;
import dev.aperture.geometry.recipe.shape.ShapeRecipe;
import dev.aperture.geometry.recipe.shape.SolidShapeRecipe;
import dev.aperture.geometry.recipe.shape.SubtractBoxesRecipe;
import dev.aperture.geometry.shape.BoxShape;
import dev.aperture.geometry.shape.SolidShape;
import dev.aperture.math.BoundingBox;

/**
 * Evaluates {@link ShapeRecipe} IR into eager {@link SolidShape} trees.
 */
public final class ShapeRecipeEvaluator {
	private ShapeRecipeEvaluator() {
	}

	public static SolidShape evaluate(ShapeRecipe recipe) {
		return switch (recipe) {
			case BoxRecipe(var bounds) -> new BoxShape(bounds);
			case ExtrudeLinearRecipe(var profile, var pathStart, var pathEnd, var profileU, var profileV) ->
				ExtrudeOp.linear(profile, pathStart, pathEnd, profileU, profileV);
			case SubtractBoxesRecipe(var base, var subtractBoxes) ->
				BooleanOp.subtractBoxes(evaluate(base), subtractBoxes.toArray(BoundingBox[]::new));
			case SolidShapeRecipe(var shape) -> shape;
		};
	}
}
