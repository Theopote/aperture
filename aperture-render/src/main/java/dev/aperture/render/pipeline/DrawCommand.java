package dev.aperture.render.pipeline;

import dev.aperture.math.Transform3d;
import dev.aperture.geometry.material.MaterialBinding;
import dev.aperture.render.mesh.MeshSection;

import java.util.Objects;
import java.util.Set;

/**
 * One draw submission for a mesh section at an instance transform.
 */
public record DrawCommand(
	MeshSection meshSection,
	Transform3d transform,
	MaterialBinding materialBinding,
	RenderPass pass,
	Set<RenderMode> modes
) {
	public DrawCommand {
		Objects.requireNonNull(meshSection, "meshSection");
		Objects.requireNonNull(transform, "transform");
		Objects.requireNonNull(materialBinding, "materialBinding");
		Objects.requireNonNull(pass, "pass");
		modes = Set.copyOf(modes);
	}
}
