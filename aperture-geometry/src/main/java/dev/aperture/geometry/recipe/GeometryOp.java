package dev.aperture.geometry.recipe;

import dev.aperture.geometry.recipe.shape.ShapeRecipe;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Transform3d;

import java.util.List;

/**
 * One assembly-level operation in a {@link GeometryRecipe}.
 */
public sealed interface GeometryOp permits EmitSolidOp, SetCutVolumeOp {
}
