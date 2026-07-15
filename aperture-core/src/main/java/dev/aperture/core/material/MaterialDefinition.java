package dev.aperture.core.material;

import java.util.Objects;

/**
 * Platform-agnostic material description. Fabric adapter maps this to textures and shaders.
 */
public record MaterialDefinition(
	String id,
	String albedoTexture,
	float roughness,
	float metalness,
	BlendMode blendMode,
	boolean doubleSided
) {
	public MaterialDefinition {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(albedoTexture, "albedoTexture");
		Objects.requireNonNull(blendMode, "blendMode");
	}
}
