package dev.aperture.api.registry;

import dev.aperture.render.material.MaterialDefinition;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of catalog materials and default slot bindings loaded from data packs.
 */
public final class MaterialCatalogRegistry {
	private final Map<String, MaterialDefinition> materials = new ConcurrentHashMap<>();
	private final Map<String, String> slotDefaults = new ConcurrentHashMap<>();

	private static final MaterialDefinition FALLBACK = new MaterialDefinition(
		"aperture:vanilla/fallback",
		"minecraft:block/stone",
		0.5f,
		0.0f,
		dev.aperture.render.material.BlendMode.OPAQUE,
		false
	);

	public void register(MaterialDefinition definition) {
		Objects.requireNonNull(definition, "definition");
		materials.put(definition.id(), definition);
	}

	public void registerSlotDefault(String slot, String materialId) {
		Objects.requireNonNull(slot, "slot");
		Objects.requireNonNull(materialId, "materialId");
		slotDefaults.put(slot, materialId);
	}

	public Optional<MaterialDefinition> get(String materialId) {
		return Optional.ofNullable(materials.get(materialId));
	}

	public MaterialDefinition resolveSlot(String slot) {
		String materialId = slotDefaults.get(slot);
		if (materialId == null) {
			return FALLBACK;
		}
		return materials.getOrDefault(materialId, FALLBACK);
	}
}
