package dev.aperture.geometry.recipe.shape;

import dev.aperture.math.Vec3d;
import dev.aperture.geometry.profile.ProfileCurve;

public record ExtrudeLinearRecipe(
	ProfileCurve profile,
	Vec3d pathStart,
	Vec3d pathEnd,
	Vec3d profileU,
	Vec3d profileV
) implements ShapeRecipe {
}
