package dev.aperture.geometry.pipeline;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;

/**
 * Complete output of the opening generator pipeline.
 */
public record PipelineResult(
	GeometryResult geometry,
	MeshAssembly meshes
) {
}
