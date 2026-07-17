package dev.aperture.runtime;

import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.instance.OpeningInstanceStore;
import dev.aperture.core.placement.PlacementService;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.runtime.catalog.MaterialCatalogRegistry;
import dev.aperture.runtime.registry.MaterialResolverRegistry;
import dev.aperture.runtime.pipeline.RuntimePipeline;
import dev.aperture.runtime.pipeline.RuntimeInteraction;
import dev.aperture.runtime.pipeline.RuntimeResult;
import dev.aperture.runtime.service.OpeningGenerationService;

import java.util.UUID;

/**
 * Server-safe runtime facade: opening generation, placement, persistence, and render data.
 * Does not include editor services (selection, gizmos, inspector, undo).
 */
public final class ApertureRuntime {
	private static ApertureRuntime instance;

	private final OpeningTypeRegistry openingTypes;
	private final ProfileCatalogRegistry profiles;
	private final MaterialCatalogRegistry materialCatalog;
	private final MaterialResolverRegistry materials;
	private final OpeningInstanceStore instances;
	private final OpeningGenerationService generation;
	private final PlacementService placement;
	private final ArchitecturalRuntimeEnvironment environment;
	private final RuntimePipeline runtimePipeline;

	public ApertureRuntime(
		OpeningTypeRegistry openingTypes,
		ProfileCatalogRegistry profiles,
		MaterialCatalogRegistry materialCatalog,
		MaterialResolverRegistry materials,
		OpeningInstanceStore instances,
		OpeningGenerationService generation,
		PlacementService placement,
		RuntimePipeline runtimePipeline,
		ArchitecturalRuntimeEnvironment environment
	) {
		this.openingTypes = openingTypes;
		this.profiles = profiles;
		this.materialCatalog = materialCatalog;
		this.materials = materials;
		this.instances = instances;
		this.generation = generation;
		this.placement = placement;
		this.runtimePipeline = runtimePipeline;
		this.environment = environment;
	}

	public static void init(ApertureRuntime runtime) {
		instance = runtime;
	}

	public static ApertureRuntime get() {
		if (instance == null) {
			throw new IllegalStateException("ApertureRuntime has not been initialized");
		}
		return instance;
	}

	public OpeningTypeRegistry openingTypes() {
		return openingTypes;
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

	public PlacementService placement() {
		return placement;
	}

	public RuntimePipeline runtimePipeline() {
		return runtimePipeline;
	}

	public ArchitecturalRuntimeEnvironment environment() {
		return environment;
	}

	public RuntimeResult interact(UUID objectId, RuntimeInteraction interaction) {
		return environment.interact(objectId, interaction);
	}

	public void scheduleInteraction(long delayTicks, UUID objectId, RuntimeInteraction interaction) {
		environment.scheduleInteraction(delayTicks, objectId, interaction);
	}

	public int tick() {
		return environment.tick();
	}
}
