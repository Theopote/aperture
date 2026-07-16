package dev.aperture.geometry.pipeline.mesh;

import dev.aperture.math.BoundingBox;
import dev.aperture.geometry.mesh.Mesh;

import java.util.Map;

/**
 * Per-part meshes produced by {@link MeshAssembler}.
 */
public record MeshAssembly(
	Map<String, Mesh> partsByPath,
	BoundingBox bounds
) {
	public MeshAssembly {
		partsByPath = Map.copyOf(partsByPath);
	}

	/**
	 * Returns an empty mesh assembly (for testing/mocking).
	 */
	public static MeshAssembly empty() {
		return new MeshAssembly(Map.of(), BoundingBox.EMPTY);
	}
}
