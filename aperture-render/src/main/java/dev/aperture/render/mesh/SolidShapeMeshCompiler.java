package dev.aperture.render.mesh;

import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.mesh.ShapeMesher;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.model.PartId;

/**
 * Compiles {@link GeometrySolid} shapes into mesh sections via the geometry kernel.
 */
public final class SolidShapeMeshCompiler implements MeshCompiler {
	@Override
	public MeshSection compile(GeometrySolid solid, LODLevel level) {
		Mesh mesh = ShapeMesher.mesh(solid.shape(), solid.localTransform());
		return MeshSectionFactory.fromMesh(PartId.of(solid.componentPath()), solid.layer(), mesh);
	}

	@Override
	public boolean supports(GeometrySolid solid) {
		return solid != null;
	}
}
