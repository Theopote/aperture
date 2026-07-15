package dev.aperture.render.material;

import dev.aperture.geometry.material.MaterialBindingSet;
import dev.aperture.geometry.model.GeometryLayer;

/**
 * Client-side material binding filters for preview rendering.
 */
public final class MaterialBindingFilters {
	private MaterialBindingFilters() {
	}

	public static MaterialBindingSet forPreviewMode(MaterialBindingSet bindings, MaterialPreviewMode mode) {
		return switch (mode) {
			case FULL, ALBEDO, NORMALS, WIREFRAME -> bindings;
			case FRAME_ONLY -> bindings.filtered(binding -> binding.layer() != GeometryLayer.TRANSLUCENT);
			case GLASS_ONLY -> bindings.filtered(binding -> binding.layer() == GeometryLayer.TRANSLUCENT);
		};
	}
}
