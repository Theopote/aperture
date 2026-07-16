package dev.aperture.geometry.pipeline;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.geometry.recipe.GeometryRecipe;
import dev.aperture.math.BoundingBox;
import org.jspecify.annotations.Nullable;

/**
 * Complete output of procedural geometry generation (solids + baked meshes + optional recipe IR).
 */
public record PipelineResult(
	GeometryResult geometry,
	MeshAssembly meshes,
	@Nullable GeometryRecipe recipe,
	@Nullable BoundingBox collision,
	@Nullable BoundingBox footprint
) {
	public PipelineResult(GeometryResult geometry, MeshAssembly meshes) {
		this(geometry, meshes, null, null, null);
	}

	public PipelineResult(GeometryResult geometry, MeshAssembly meshes, GeometryRecipe recipe) {
		this(geometry, meshes, recipe, null, null);
	}

	public PipelineResult withCollision(BoundingBox collision) {
		return new PipelineResult(geometry, meshes, recipe, collision, footprint);
	}

	public PipelineResult withFootprint(BoundingBox footprint) {
		return new PipelineResult(geometry, meshes, recipe, collision, footprint);
	}

	public PipelineResult withCollisionAndFootprint(BoundingBox collision, BoundingBox footprint) {
		return new PipelineResult(geometry, meshes, recipe, collision, footprint);
	}

	/**
	 * Returns an empty pipeline result (for testing/mocking).
	 */
	public static PipelineResult empty() {
		return new PipelineResult(
			GeometryResult.empty(),
			MeshAssembly.empty(),
			null,
			null,
			null
		);
	}
}
