package dev.aperture.render.mesh;

import dev.aperture.math.BoundingBox;
import dev.aperture.geometry.model.GeometrySolid;
import dev.aperture.geometry.model.PartId;

/**
 * Phase 0 mesh compiler: axis-aligned bounding box to 12 triangles.
 *
 * @deprecated Use {@link SolidShapeMeshCompiler} for all solid shapes.
 */
@Deprecated
public final class BoxMeshCompiler implements MeshCompiler {
	private final SolidShapeMeshCompiler delegate = new SolidShapeMeshCompiler();

	@Override
	public MeshSection compile(GeometrySolid solid, LODLevel level) {
		return delegate.compile(solid, level);
	}

	@Override
	public boolean supports(GeometrySolid solid) {
		return delegate.supports(solid);
	}

	public MeshSection compileBox(PartId partId, BoundingBox bounds, dev.aperture.geometry.model.GeometryLayer layer) {
		GeometrySolid solid = GeometrySolid.box(partId.componentPath(), "frame", layer, bounds);
		MeshSection section = delegate.compile(solid, LODLevel.FULL);
		return new MeshSection(partId, layer, section.vertices(), section.indices(), section.bounds(), section.handle());
	}
}
