package dev.aperture.geometry.pipeline;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;

/**
 * Complete output of procedural geometry generation (solids + baked meshes).
 */
public record PipelineResult(
	GeometryResult geometry,
	MeshAssembly meshes
) {
}
