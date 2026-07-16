package dev.aperture.pipeline.stage;

import dev.aperture.geometry.mesh.ShapeMesher;
import dev.aperture.geometry.mesh.TriangleMesh;
import dev.aperture.geometry.model.CompositeGeometry;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mesh generation stage.
 * <p>
 * Converts the composite geometry into triangle meshes suitable for rendering.
 * Each solid in the geometry is meshed separately and stored in a map keyed
 * by layer name.
 * <p>
 * Input: {@link CompositeGeometry} (opening geometry)
 * Output: {@link MeshCollection} (triangle meshes for each layer)
 */
public final class MeshStage implements PipelineStage<CompositeGeometry, MeshStage.MeshCollection> {

	private final ShapeMesher mesher;

	/**
	 * Create mesh stage with default mesher.
	 */
	public MeshStage() {
		this(new ShapeMesher());
	}

	/**
	 * Create mesh stage with custom mesher.
	 */
	public MeshStage(ShapeMesher mesher) {
		this.mesher = Objects.requireNonNull(mesher, "mesher cannot be null");
	}

	@Override
	public String name() {
		return "mesh";
	}

	@Override
	public StageResult<MeshCollection> execute(CompositeGeometry input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");

		ctx.debug("Generating meshes for " + input.solids().size() + " solids");

		try {
			Map<String, TriangleMesh> meshes = new HashMap<>();

			// Mesh each solid
			for (var solid : input.solids()) {
				String layerName = solid.layer().name();

				ctx.debug("Meshing layer: " + layerName);

				TriangleMesh mesh = mesher.mesh(solid.shape());
				meshes.put(layerName, mesh);

				ctx.debug("Generated mesh with " + mesh.triangleCount() + " triangles");
			}

			return new StageResult.Success<>(new MeshCollection(meshes));

		} catch (Exception e) {
			return new StageResult.Failure<>(
				"Failed to generate meshes: " + e.getMessage(),
				e
			);
		}
	}

	/**
	 * Collection of meshes organized by layer name.
	 */
	public record MeshCollection(Map<String, TriangleMesh> meshes) {
		public MeshCollection {
			Objects.requireNonNull(meshes, "meshes cannot be null");
			meshes = Map.copyOf(meshes); // Immutable
		}

		/**
		 * Get mesh for a specific layer.
		 */
		public TriangleMesh getMesh(String layerName) {
			return meshes.get(layerName);
		}

		/**
		 * Get total triangle count across all meshes.
		 */
		public int totalTriangleCount() {
			return meshes.values().stream()
				.mapToInt(TriangleMesh::triangleCount)
				.sum();
		}
	}
}
