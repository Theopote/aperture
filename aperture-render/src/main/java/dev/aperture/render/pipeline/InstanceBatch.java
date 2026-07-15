package dev.aperture.render.pipeline;

import dev.aperture.render.material.MaterialInstance;
import dev.aperture.render.mesh.LODLevel;
import dev.aperture.render.mesh.MeshSection;

import java.util.List;
import java.util.Objects;

/**
 * Instanced draw batch sharing mesh, material, LOD, and pass.
 */
public record InstanceBatch(
	MeshSection meshSection,
	MaterialInstance material,
	LODLevel lod,
	RenderPass pass,
	List<BatchInstance> instances
) {
	public InstanceBatch {
		Objects.requireNonNull(meshSection, "meshSection");
		Objects.requireNonNull(material, "material");
		Objects.requireNonNull(lod, "lod");
		Objects.requireNonNull(pass, "pass");
		instances = List.copyOf(instances);
	}

	public record BatchInstance(long instanceId, float[] transformMatrix4x4) {
		public BatchInstance {
			if (transformMatrix4x4.length != 16) {
				throw new IllegalArgumentException("transformMatrix4x4 must have length 16");
			}
		}
	}
}
