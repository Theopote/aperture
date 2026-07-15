package dev.aperture.geometry.pipeline.assembly;

import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.recipe.shape.ShapeRecipe;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Transform3d;

/**
 * Target for pipeline steps that emit geometry — either eager ({@link GeometryAssemblyBuilder})
 * or declarative ({@link dev.aperture.geometry.recipe.GeometryRecipeBuilder}).
 */
public interface GeometryCompilationTarget {
	void addSolid(GeometrySolid solid);

	void setCutVolume(BoundingBox cutVolume);

	void emitSolid(String componentPath, String materialSlot, GeometryLayer layer, ShapeRecipe shape);

	void emitSolid(
		String componentPath,
		String materialSlot,
		GeometryLayer layer,
		ShapeRecipe shape,
		Transform3d localTransform
	);
}
