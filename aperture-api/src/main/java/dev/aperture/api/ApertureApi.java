package dev.aperture.api;

import dev.aperture.api.catalog.MaterialCatalogRegistry;
import dev.aperture.api.registry.GeneratorRegistry;
import dev.aperture.api.registry.MaterialResolverRegistry;
import dev.aperture.api.service.OpeningGenerationService;
import dev.aperture.api.service.ParametricService;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.instance.OpeningInstanceStore;
import dev.aperture.core.placement.PlacementService;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;

/**
 * Public API surface for Aperture addon mods.
 */
public final class ApertureApi {
	private static ApertureApi instance;

	private final OpeningTypeRegistry openingTypes;
	private final GeneratorRegistry generators;
	private final ProfileCatalogRegistry profiles;
	private final MaterialCatalogRegistry materialCatalog;
	private final MaterialResolverRegistry materials;
	private final OpeningInstanceStore instances;
	private final OpeningGenerationService generation;
	private final ParametricService parametrics;
	private final PlacementService placement;

	public ApertureApi(
		OpeningTypeRegistry openingTypes,
		GeneratorRegistry generators,
		ProfileCatalogRegistry profiles,
		MaterialCatalogRegistry materialCatalog,
		MaterialResolverRegistry materials,
		OpeningInstanceStore instances,
		OpeningGenerationService generation,
		PlacementService placement
	) {
		this(openingTypes, generators, profiles, materialCatalog, materials, instances, generation, new ParametricService(), placement);
	}

	public ApertureApi(
		OpeningTypeRegistry openingTypes,
		GeneratorRegistry generators,
		ProfileCatalogRegistry profiles,
		MaterialCatalogRegistry materialCatalog,
		MaterialResolverRegistry materials,
		OpeningInstanceStore instances,
		OpeningGenerationService generation,
		ParametricService parametrics,
		PlacementService placement
	) {
		this.openingTypes = openingTypes;
		this.generators = generators;
		this.profiles = profiles;
		this.materialCatalog = materialCatalog;
		this.materials = materials;
		this.instances = instances;
		this.generation = generation;
		this.parametrics = parametrics;
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

	public ProfileCatalogRegistry profiles() {
		return profiles;
	}

	public MaterialCatalogRegistry materialCatalog() {
		return materialCatalog;
	}

	public MaterialResolverRegistry materials() {
		return materials;
	}

	public OpeningInstanceStore instances() {
		return instances;
	}

	public OpeningGenerationService generation() {
		return generation;
	}

	public ParametricService parametrics() {
		return parametrics;
	}

	public PlacementService placement() {
		return placement;
	}
}
