package dev.aperture.geometry.pipeline;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.geometry.recipe.GeometryRecipe;

/**
 * Complete output of procedural geometry generation (solids + baked meshes + optional recipe IR).
 */
public record PipelineResult(
	GeometryResult geometry,
	MeshAssembly meshes,
	GeometryRecipe recipe
) {
	public PipelineResult(GeometryResult geometry, MeshAssembly meshes) {
		this(geometry, meshes, null);
	}
}
