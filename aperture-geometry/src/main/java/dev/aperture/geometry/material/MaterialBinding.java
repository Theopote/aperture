package dev.aperture.geometry.material;

import dev.aperture.core.material.BlendMode;
import dev.aperture.core.material.MaterialInstance;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.geometry.model.PartId;

import java.util.Objects;

/**
 * Binds a part or layer to a resolved material instance.
 */
public record MaterialBinding(
	PartId partId,
	String materialSlot,
	GeometryLayer layer,
	MaterialInstance material,
	BlendMode blendMode
) {
	public MaterialBinding {
		Objects.requireNonNull(partId, "partId");
		Objects.requireNonNull(materialSlot, "materialSlot");
		Objects.requireNonNull(layer, "layer");
		Objects.requireNonNull(material, "material");
		Objects.requireNonNull(blendMode, "blendMode");
	}
}
