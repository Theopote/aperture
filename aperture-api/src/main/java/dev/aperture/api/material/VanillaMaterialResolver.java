package dev.aperture.api.material;

import dev.aperture.geometry.model.GeometryLayer;
import dev.aperture.render.material.BlendMode;
import dev.aperture.render.material.MaterialDefinition;
import dev.aperture.render.material.MaterialInstance;

import java.util.Map;
import java.util.Optional;

/**
 * Built-in fallback resolver used when no catalog is available (tests and dev bootstrap safety).
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
		Optional<String> overrideTexture = MaterialResolveSupport.materialRefOverride(context);
		MaterialDefinition resolved = overrideTexture
			.map(texture -> MaterialResolveSupport.withAlbedo(base, texture))
			.orElse(base);
		MaterialDefinition layerAdjusted = MaterialResolveSupport.adjustForLayer(resolved, context.layer());
		return MaterialInstance.of(layerAdjusted);
	}

	static String toAlbedoTexture(String materialRef) {
		return MaterialResolveSupport.toAlbedoTexture(materialRef);
	}
}
