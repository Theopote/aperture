package dev.aperture.render.mesh;

import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.mesh.ShapeMesher;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.render.data.PartId;

/**
 * Compiles {@link GeometrySolid} shapes into mesh sections via the geometry kernel.
 */
public final class SolidShapeMeshCompiler implements MeshCompiler {
	@Override
	public MeshSection compile(GeometrySolid solid, LODLevel level) {
		Mesh mesh = ShapeMesher.mesh(solid.shape(), solid.localTransform());
		return new MeshSection(
			PartId.of(solid.componentPath()),
			solid.layer(),
			mesh.vertices(),
			mesh.indices(),
			mesh.bounds(),
			MeshHandle.next()
		);
	}

	@Override
	public boolean supports(GeometrySolid solid) {
		return solid != null;
	}
}
