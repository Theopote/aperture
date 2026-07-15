package dev.aperture.geometry.recipe.shape;

import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.geometry.profile.ProfileCurve;
import dev.aperture.geometry.shape.SolidShape;

import java.util.List;

public final class ShapeRecipes {
	private ShapeRecipes() {
	}

	public static ShapeRecipe box(BoundingBox bounds) {
		return new BoxRecipe(bounds);
	}

	public static ShapeRecipe extrudeLinear(
		ProfileCurve profile,
		Vec3d pathStart,
		Vec3d pathEnd,
		Vec3d profileU,
		Vec3d profileV
	) {
		return new ExtrudeLinearRecipe(profile, pathStart, pathEnd, profileU, profileV);
	}

	public static ShapeRecipe extrudeLinear(
		ProfileCurve profile,
		Vec3d pathStart,
		Vec3d pathEnd,
		Vec3d profileU,
		Vec3d profileV,
		BoundingBox... subtractBoxes
	) {
		ShapeRecipe base = extrudeLinear(profile, pathStart, pathEnd, profileU, profileV);
		if (subtractBoxes.length == 0) {
			return base;
		}
		return new SubtractBoxesRecipe(base, List.of(subtractBoxes));
	}

	public static ShapeRecipe union(List<ShapeRecipe> operands) {
		return new UnionRecipe(operands);
	}

	public static ShapeRecipe union(ShapeRecipe... operands) {
		return new UnionRecipe(List.of(operands));
	}

	public static ShapeRecipe fromSolid(SolidShape shape) {
		return new SolidShapeRecipe(shape);
	}
}
