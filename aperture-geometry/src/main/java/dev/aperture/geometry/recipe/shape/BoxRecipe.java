package dev.aperture.geometry.recipe.shape;

import dev.aperture.math.BoundingBox;

public record BoxRecipe(BoundingBox bounds) implements ShapeRecipe {
}
