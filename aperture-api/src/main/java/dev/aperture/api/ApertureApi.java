package dev.aperture.api;

import dev.aperture.api.registry.GeneratorRegistry;
import dev.aperture.api.service.OpeningGenerationService;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.instance.OpeningInstanceStore;
import dev.aperture.core.placement.PlacementService;

/**
 * Public API surface for Aperture addon mods.
 */
public final class ApertureApi {
	private static ApertureApi instance;

	private final OpeningTypeRegistry openingTypes;
	private final GeneratorRegistry generators;
	private final OpeningInstanceStore instances;
	private final OpeningGenerationService generation;
	private final PlacementService placement;

	public ApertureApi(
		OpeningTypeRegistry openingTypes,
		GeneratorRegistry generators,
		OpeningInstanceStore instances,
		OpeningGenerationService generation,
		PlacementService placement
	) {
		this.openingTypes = openingTypes;
		this.generators = generators;
		this.instances = instances;
		this.generation = generation;
		this.placement = placement;
	}

	public static void init(ApertureApi api) {
		instance = api;
	}

	public static ApertureApi get() {
		if (instance == null) {
			throw new IllegalStateException("ApertureApi has not been initialized");
		}
		return instance;
	}

	public OpeningTypeRegistry openingTypes() {
		return openingTypes;
	}

	public GeneratorRegistry generators() {
		return generators;
	}

	public OpeningInstanceStore instances() {
		return instances;
	}

	public OpeningGenerationService generation() {
		return generation;
	}

	public PlacementService placement() {
		return placement;
	}
}
