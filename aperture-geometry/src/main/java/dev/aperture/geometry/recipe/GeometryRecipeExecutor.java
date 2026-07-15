package dev.aperture.geometry.recipe;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.math.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes a {@link GeometryRecipe} through the geometry kernel.
 */
public final class GeometryRecipeExecutor {
	private GeometryRecipeExecutor() {
	}

	public static GeometryResult execute(GeometryRecipe recipe) {
		List<GeometrySolid> solids = new ArrayList<>();
		BoundingBox bounds = null;
		BoundingBox cutVolume = null;

		for (GeometryOp op : recipe.ops()) {
			switch (op) {
				case EmitSolidOp emit -> {
					var shape = ShapeRecipeEvaluator.evaluate(emit.shape());
					GeometrySolid solid = GeometrySolid.of(
						emit.componentPath(),
						emit.materialSlot(),
						emit.layer(),
						shape,
						emit.localTransform()
					);
					solids.add(solid);
					bounds = bounds == null ? solid.bounds() : bounds.union(solid.bounds());
				}
				case SetCutVolumeOp(var volume) -> cutVolume = volume;
			}
		}

		if (solids.isEmpty()) {
			throw new IllegalStateException("recipe produced no solids");
		}
		if (bounds == null || cutVolume == null) {
			throw new IllegalStateException("recipe bounds or cut volume were not set");
		}
		return new GeometryResult(List.copyOf(solids), bounds, cutVolume);
	}
}
