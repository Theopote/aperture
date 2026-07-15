package dev.aperture.geometry.recipe.shape;

import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.shape.SolidShape;

import java.util.List;

/**
 * Domain-agnostic shape construction IR. Mirrors {@link SolidShape} but stays declarative
 * for serialization, NodeCraft graphs, and deferred kernel evaluation.
 */
public sealed interface ShapeRecipe permits
	BoxRecipe,
	ExtrudeLinearRecipe,
	SubtractBoxesRecipe,
	SolidShapeRecipe {
}
