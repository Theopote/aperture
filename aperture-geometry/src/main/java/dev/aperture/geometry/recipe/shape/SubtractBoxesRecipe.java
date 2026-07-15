package dev.aperture.geometry.recipe.shape;

import dev.aperture.math.BoundingBox;

import java.util.List;

public record SubtractBoxesRecipe(ShapeRecipe base, List<BoundingBox> subtractBoxes) implements ShapeRecipe {
	public SubtractBoxesRecipe {
		subtractBoxes = List.copyOf(subtractBoxes);
	}
}
