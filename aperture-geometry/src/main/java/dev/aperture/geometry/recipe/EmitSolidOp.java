package dev.aperture.geometry.recipe;

import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.recipe.shape.ShapeRecipe;
import dev.aperture.math.Transform3d;

public record EmitSolidOp(
	String componentPath,
	String materialSlot,
	GeometryLayer layer,
	ShapeRecipe shape,
	Transform3d localTransform
) implements GeometryOp {
	public EmitSolidOp {
		if (localTransform == null) {
			localTransform = Transform3d.identity();
		}
	}
}
