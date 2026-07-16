package dev.aperture.opening.geometry.pipeline;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.geometry.recipe.GeometryRecipe;

/**
 * Full compile output: declarative recipe plus evaluated geometry and meshes.
 */
public record CompiledPipeline(
	GeometryRecipe recipe,
	GeometryResult geometry,
	MeshAssembly meshes
) {
}
