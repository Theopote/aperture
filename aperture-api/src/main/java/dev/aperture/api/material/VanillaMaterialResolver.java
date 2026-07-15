package dev.aperture.api.material;

import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;
import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.render.material.BlendMode;
import dev.aperture.render.material.MaterialDefinition;
import dev.aperture.render.material.MaterialInstance;

import java.util.Map;
import java.util.Optional;

/**
 * Phase 1 resolver mapping opening material slots to vanilla block texture ids.
 */
public final class VanillaMaterialResolver implements MaterialResolver {
	public static final VanillaMaterialResolver INSTANCE = new VanillaMaterialResolver();

	private static final Map<String, MaterialDefinition> DEFAULTS = Map.of(
		"frame", new MaterialDefinition(
			"aperture:vanilla/frame_oak",
			"minecraft:block/oak_planks",
			0.7f,
			0.0f,
			BlendMode.OPAQUE,
			false
		),
		"glazing", new MaterialDefinition(
			"aperture:vanilla/glazing_clear",
			"minecraft:block/light_blue_stained_glass",
			0.1f,
			0.0f,
			BlendMode.TRANSLUCENT,
			false
		),
		"hardware", new MaterialDefinition(
			"aperture:vanilla/hardware_iron",
			"minecraft:block/iron_block",
			0.4f,
			0.8f,
			BlendMode.CUTOUT,
			false
		)
	);

	private static final MaterialDefinition FALLBACK = new MaterialDefinition(
		"aperture:vanilla/fallback",
		"minecraft:block/stone",
		0.5f,
		0.0f,
		BlendMode.OPAQUE,
		false
	);

	private VanillaMaterialResolver() {
	}

	@Override
	public MaterialInstance resolve(MaterialResolveContext context) {
		MaterialDefinition base = DEFAULTS.getOrDefault(context.materialSlot(), FALLBACK);
		Optional<String> overrideTexture = materialRefOverride(context);
		MaterialDefinition resolved = overrideTexture
			.map(texture -> withAlbedo(base, texture))
			.orElse(base);
		MaterialDefinition layerAdjusted = adjustForLayer(resolved, context.layer());
		return MaterialInstance.of(layerAdjusted);
	}

	private static MaterialDefinition withAlbedo(MaterialDefinition base, String albedoTexture) {
		return new MaterialDefinition(
			base.id(),
			albedoTexture,
			base.roughness(),
			base.metalness(),
			base.blendMode(),
			base.doubleSided()
		);
	}

	private static MaterialDefinition adjustForLayer(MaterialDefinition definition, GeometryLayer layer) {
		BlendMode blendMode = switch (layer) {
			case OPAQUE_FRAME -> BlendMode.OPAQUE;
			case CUTOUT_HARDWARE -> BlendMode.CUTOUT;
			case TRANSLUCENT_GLASS -> BlendMode.TRANSLUCENT;
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

	private static Optional<String> materialRefOverride(MaterialResolveContext context) {
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

	static String toAlbedoTexture(String materialRef) {
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
