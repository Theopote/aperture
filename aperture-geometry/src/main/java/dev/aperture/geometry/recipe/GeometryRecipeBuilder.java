package dev.aperture.geometry.recipe;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.pipeline.assembly.GeometryCompilationTarget;
import dev.aperture.geometry.recipe.shape.ShapeRecipe;
import dev.aperture.geometry.recipe.shape.SolidShapeRecipe;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Transform3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable accumulator for {@link GeometryRecipe} ops. Implements the same surface as
 * {@link dev.aperture.geometry.pipeline.assembly.GeometryAssemblyBuilder} for pipeline steps.
 */
public final class GeometryRecipeBuilder implements GeometryCompilationTarget {
	private final List<GeometryOp> ops = new ArrayList<>();
	private BoundingBox cutVolume;

	@Override
	public void emitSolid(
		String componentPath,
		String materialSlot,
		GeometryLayer layer,
		ShapeRecipe shape
	) {
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
		ops.add(new EmitSolidOp(componentPath, materialSlot, layer, shape, localTransform));
	}

	@Override
	public void addSolid(GeometrySolid solid) {
		ops.add(new EmitSolidOp(
			solid.componentPath(),
			solid.materialSlot(),
			solid.layer(),
			new SolidShapeRecipe(solid.shape()),
			solid.localTransform()
		));
	}

	@Override
	public void setCutVolume(BoundingBox cutVolume) {
		this.cutVolume = cutVolume;
	}

	public GeometryRecipe build() {
		List<GeometryOp> compiled = new ArrayList<>(ops);
		if (cutVolume != null) {
			compiled.add(new SetCutVolumeOp(cutVolume));
		}
		return new GeometryRecipe(compiled);
	}

	public GeometryResult execute() {
		return GeometryRecipeExecutor.execute(build());
	}
}
