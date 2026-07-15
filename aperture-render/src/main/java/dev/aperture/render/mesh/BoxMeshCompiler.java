package dev.aperture.render.mesh;

import dev.aperture.core.geometry.BoundingBox;
import dev.aperture.core.geometry.Vec3d;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.render.data.PartId;

/**
 * Phase 0 mesh compiler: axis-aligned bounding box to 12 triangles.
 */
public final class BoxMeshCompiler implements MeshCompiler {
	@Override
	public MeshSection compile(GeometrySolid solid, LODLevel level) {
		return compileBox(PartId.of(solid.componentPath()), solid.bounds(), solid.layer());
	}

	@Override
	public boolean supports(GeometrySolid solid) {
		return solid != null;
	}

	public MeshSection compileBox(PartId partId, BoundingBox bounds, dev.aperture.geometry.model.GeometryLayer layer) {
		Vec3d min = bounds.min();
		Vec3d max = bounds.max();

		float x0 = (float) min.x();
		float y0 = (float) min.y();
		float z0 = (float) min.z();
		float x1 = (float) max.x();
		float y1 = (float) max.y();
		float z1 = (float) max.z();

		float[] vertices = {
			// -Z face
			x0, y0, z0, 0, 0, -1, 0, 0,
			x1, y0, z0, 0, 0, -1, 1, 0,
			x1, y1, z0, 0, 0, -1, 1, 1,
			x0, y1, z0, 0, 0, -1, 0, 1,
			// +Z face
			x0, y0, z1, 0, 0, 1, 0, 0,
			x1, y0, z1, 0, 0, 1, 1, 0,
			x1, y1, z1, 0, 0, 1, 1, 1,
			x0, y1, z1, 0, 0, 1, 0, 1,
		};

		int[] indices = {
			0, 1, 2, 0, 2, 3,
			5, 4, 7, 5, 7, 6,
			4, 0, 3, 4, 3, 7,
			1, 5, 6, 1, 6, 2,
			3, 2, 6, 3, 6, 7,
			4, 5, 1, 4, 1, 0,
		};

		return new MeshSection(partId, layer, vertices, indices, bounds, MeshHandle.next());
	}
}
