package dev.aperture.runtime.material;

import dev.aperture.core.material.BlendMode;
import dev.aperture.core.material.MaterialDefinition;

import java.util.Objects;

/**
 * Data-pack definition for a reusable material entry.
 */
public record MaterialCatalogEntry(
	String id,
	String albedoTexture,
	float roughness,
	float metalness,
	BlendMode blendMode,
	boolean doubleSided
) {
	public MaterialCatalogEntry {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(albedoTexture, "albedoTexture");
		Objects.requireNonNull(blendMode, "blendMode");
	}

	public MaterialDefinition toDefinition() {
		return new MaterialDefinition(id, albedoTexture, roughness, metalness, blendMode, doubleSided);
	}
}
