package dev.aperture.render.mesh;

import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.render.data.PartId;

/**
 * Converts geometry-kernel meshes into render mesh sections.
 */
public final class MeshSectionFactory {
	private MeshSectionFactory() {
	}

	public static MeshSection fromMesh(PartId partId, GeometryLayer layer, Mesh mesh) {
		return new MeshSection(
			partId,
			layer,
			mesh.vertices(),
			mesh.indices(),
			mesh.bounds(),
			MeshHandle.next()
		);
	}
}
