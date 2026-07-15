package dev.aperture.runtime.material;

import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.render.material.BlendMode;
import dev.aperture.render.material.MaterialDefinition;

import java.util.Optional;

/**
 * Shared material resolution helpers.
 */
public final class MaterialResolveSupport {
	private MaterialResolveSupport() {
	}

	public static Optional<String> materialRefOverride(MaterialResolveContext context) {
		String slot = context.materialSlot();
		Optional<ParameterValue> direct = context.parameters().get(slot)
			.filter(value -> value.type() == ParameterType.MATERIAL_REF);
		if (direct.isPresent()) {
			return Optional.of(toAlbedoTexture(((ParameterValue.MaterialRefValue) direct.get()).raw()));
		}

		Optional<ParameterValue> suffixed = context.parameters().get(slot + "_material")
			.filter(value -> value.type() == ParameterType.MATERIAL_REF);
		return suffixed.map(value -> toAlbedoTexture(((ParameterValue.MaterialRefValue) value).raw()));
	}

	public static MaterialDefinition adjustForLayer(MaterialDefinition definition, GeometryLayer layer) {
		BlendMode blendMode = switch (layer) {
			case OPAQUE -> BlendMode.OPAQUE;
			case CUTOUT -> BlendMode.CUTOUT;
			case TRANSLUCENT -> BlendMode.TRANSLUCENT;
		};
		if (definition.blendMode() == blendMode) {
			return definition;
		}
		return new MaterialDefinition(
			definition.id(),
			definition.albedoTexture(),
			definition.roughness(),
			definition.metalness(),
			blendMode,
			definition.doubleSided()
		);
	}

	public static MaterialDefinition withAlbedo(MaterialDefinition base, String albedoTexture) {
		return new MaterialDefinition(
			base.id(),
			albedoTexture,
			base.roughness(),
			base.metalness(),
			base.blendMode(),
			base.doubleSided()
		);
	}

	public static String toAlbedoTexture(String materialRef) {
		if (materialRef == null || materialRef.isBlank()) {
			throw new IllegalArgumentException("materialRef must not be blank");
		}
		if (materialRef.contains("/")) {
			return materialRef;
		}
		int colon = materialRef.indexOf(':');
		if (colon >= 0) {
			String namespace = materialRef.substring(0, colon);
			String path = materialRef.substring(colon + 1);
			return namespace + ":block/" + path;
		}
		return "minecraft:block/" + materialRef;
	}
}
