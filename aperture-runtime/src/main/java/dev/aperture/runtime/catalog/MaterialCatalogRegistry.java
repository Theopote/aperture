package dev.aperture.runtime.catalog;

import dev.aperture.render.material.BlendMode;
import dev.aperture.render.material.MaterialDefinition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry of catalog material definitions and per-slot defaults.
 */
public final class MaterialCatalogRegistry {
	private static final MaterialDefinition FALLBACK = new MaterialDefinition(
		"aperture:vanilla/fallback",
		"minecraft:block/stone",
		0.5f,
		0.0f,
		BlendMode.OPAQUE,
		false
	);

	private final Map<String, MaterialDefinition> byId = new LinkedHashMap<>();
	private final Map<String, String> slotDefaults = new LinkedHashMap<>();

	public void register(MaterialDefinition definition) {
		byId.put(definition.id(), definition);
	}

	public void setSlotDefault(String slot, String materialId) {
		slotDefaults.put(slot, materialId);
	}

	public Optional<MaterialDefinition> findById(String materialId) {
		return Optional.ofNullable(byId.get(materialId));
	}

	public MaterialDefinition requireById(String materialId) {
		return findById(materialId)
			.orElseThrow(() -> new IllegalArgumentException("Unknown catalog material: " + materialId));
	}

	public Optional<MaterialDefinition> findDefaultForSlot(String slot) {
		return Optional.ofNullable(slotDefaults.get(slot)).flatMap(this::findById);
	}

	public MaterialDefinition resolveSlot(String slot) {
		return findDefaultForSlot(slot).orElse(FALLBACK);
	}

	public Map<String, MaterialDefinition> all() {
		return Collections.unmodifiableMap(byId);
	}

	public Map<String, String> slotDefaults() {
		return Collections.unmodifiableMap(slotDefaults);
	}
}
