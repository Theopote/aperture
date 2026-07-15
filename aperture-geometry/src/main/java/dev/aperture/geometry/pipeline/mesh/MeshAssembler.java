package dev.aperture.geometry.pipeline.mesh;

import dev.aperture.geometry.mesh.ShapeMesher;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.model.GeometrySolid;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compiles {@link GeometryResult} solids into per-part {@link dev.aperture.geometry.mesh.Mesh} data.
 */
public final class MeshAssembler {
	public MeshAssembly assemble(GeometryResult geometry) {
		Map<String, dev.aperture.geometry.mesh.Mesh> meshes = new LinkedHashMap<>();
		for (GeometrySolid solid : geometry.solids()) {
			meshes.put(
				solid.componentPath(),
				ShapeMesher.mesh(solid.shape(), solid.localTransform())
			);
		}
		return new MeshAssembly(meshes, geometry.bounds());
	}
}
