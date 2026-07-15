package dev.aperture.render.mesh;

import dev.aperture.geometry.model.GeometrySolid;

/**
 * Converts logical geometry solids into platform-agnostic mesh sections.
 */
public interface MeshCompiler {
	MeshSection compile(GeometrySolid solid, LODLevel level);

	boolean supports(GeometrySolid solid);
}
