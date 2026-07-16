package dev.aperture.pipeline.stage;

import dev.aperture.geometry.mesh.Mesh;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.math.BoundingBox;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Map;
import java.util.Objects;

/** Exposes the meshes already baked by the recipe-based geometry pipeline. */
public final class MeshStage implements PipelineStage<PipelineResult, MeshStage.MeshCollection> {
	@Override
	public String name() {
		return "mesh";
	}

	@Override
	public StageResult<MeshCollection> execute(PipelineResult input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");
		return new StageResult.Success<>(
			new MeshCollection(input.meshes().partsByPath(), input.meshes().bounds())
		);
	}

	public record MeshCollection(Map<String, Mesh> meshes, BoundingBox bounds) {
		public MeshCollection {
			Objects.requireNonNull(meshes, "meshes cannot be null");
			Objects.requireNonNull(bounds, "bounds cannot be null");
			meshes = Map.copyOf(meshes);
		}

		public Mesh getMesh(String path) {
			return meshes.get(path);
		}

		public int totalTriangleCount() {
			return meshes.values().stream().mapToInt(Mesh::triangleCount).sum();
		}
	}
}
