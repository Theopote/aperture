package dev.aperture.opening.mesh;

import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.mesh.MeshAssembly;
import dev.aperture.geometry.pipeline.mesh.MeshAssembler;

/**
 * Mesh builder layer: bakes evaluated geometry into per-part CPU meshes.
 */
public final class MeshBuilder {
	private final MeshAssembler assembler = new MeshAssembler();

	public MeshAssembly build(GeometryResult geometry) {
		return assembler.assemble(geometry);
	}
}
