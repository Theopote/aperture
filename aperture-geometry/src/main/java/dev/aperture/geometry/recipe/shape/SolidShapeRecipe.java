package dev.aperture.geometry.recipe.shape;

import dev.aperture.geometry.shape.SolidShape;

/**
 * Escape hatch while migrating generators: wraps an already-built {@link SolidShape}.
 */
public record SolidShapeRecipe(SolidShape shape) implements ShapeRecipe {
}
