package dev.aperture.geometry.pipeline.assembly;

import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.recipe.ShapeRecipeEvaluator;
import dev.aperture.geometry.recipe.shape.ShapeRecipe;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Transform3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable builder for assembling {@link GeometryResult} from pipeline generators.
 */
public final class GeometryAssemblyBuilder implements GeometryCompilationTarget {
	private final List<GeometrySolid> solids = new ArrayList<>();
	private BoundingBox bounds;
	private BoundingBox cutVolume;

	public void addSolid(GeometrySolid solid) {
		solids.add(solid);
		bounds = bounds == null ? solid.bounds() : bounds.union(solid.bounds());
	}

	public void setCutVolume(BoundingBox cutVolume) {
		this.cutVolume = cutVolume;
	}

	@Override
	public void emitSolid(String componentPath, String materialSlot, GeometryLayer layer, ShapeRecipe shape) {
		emitSolid(componentPath, materialSlot, layer, shape, Transform3d.identity());
	}

	@Override
	public void emitSolid(
		String componentPath,
		String materialSlot,
		GeometryLayer layer,
		ShapeRecipe shape,
		Transform3d localTransform
	) {
		addSolid(GeometrySolid.of(
			componentPath,
			materialSlot,
			layer,
			ShapeRecipeEvaluator.evaluate(shape),
			localTransform
		));
	}

	public GeometryResult build() {
		if (solids.isEmpty()) {
			throw new IllegalStateException("generation produced no solids");
		}
		if (bounds == null || cutVolume == null) {
			throw new IllegalStateException("generation bounds or cut volume were not set");
		}
		return new GeometryResult(List.copyOf(solids), bounds, cutVolume);
	}
}
