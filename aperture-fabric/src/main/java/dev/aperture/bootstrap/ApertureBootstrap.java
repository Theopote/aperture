package dev.aperture.bootstrap;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeCatalogLoader;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.InMemoryOpeningInstanceStore;
import dev.aperture.core.instance.OpeningInstanceStore;
import dev.aperture.core.placement.PlacementService;
import dev.aperture.fabric.placement.FabricPlacementAdapter;
import dev.aperture.kernel.ApertureKernel;
import dev.aperture.geometry.profile.ProfileCatalogLoader;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.registry.ApertureBlockEntities;
import dev.aperture.registry.ApertureBlocks;
import dev.aperture.runtime.ApertureRuntime;
import dev.aperture.runtime.ArchitecturalRuntimeEnvironment;
import dev.aperture.runtime.catalog.MaterialCatalogLoader;
import dev.aperture.runtime.diagnostic.RuntimeDiagnostics;
import dev.aperture.runtime.catalog.MaterialCatalogRegistry;
import dev.aperture.runtime.event.RuntimeEventBus;
import dev.aperture.runtime.material.CatalogMaterialResolver;
import dev.aperture.runtime.material.VanillaMaterialResolver;
import dev.aperture.runtime.registry.MaterialResolverRegistry;
import dev.aperture.runtime.pipeline.OpeningInstanceRepository;
import dev.aperture.runtime.pipeline.OpeningRuntimeBehavior;
import dev.aperture.runtime.pipeline.RuntimePipeline;
import dev.aperture.runtime.replication.RuntimeReplicator;
import dev.aperture.runtime.schedule.RuntimeTickScheduler;
import dev.aperture.runtime.state.RuntimeObjectRegistry;
import dev.aperture.runtime.transaction.RuntimeTransactionManager;
import dev.aperture.runtime.world.RuntimeWorldQuery;
import dev.aperture.runtime.service.OpeningGenerationService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wires runtime registries, data packs, generators, and services at mod startup.
 * Editor services are initialized separately on the client via {@link dev.aperture.editor.ApertureEditor}.
 */
public final class ApertureBootstrap {
	private static final Logger LOGGER = LoggerFactory.getLogger("aperture");

	private final OpeningTypeRegistry openingTypes = new OpeningTypeRegistry();
	private final ProfileCatalogRegistry profileCatalog = new ProfileCatalogRegistry();
	private final MaterialCatalogRegistry materialCatalog = new MaterialCatalogRegistry();
	private final MaterialResolverRegistry materials = new MaterialResolverRegistry(VanillaMaterialResolver.INSTANCE);
	private final OpeningInstanceStore instances = new InMemoryOpeningInstanceStore();
	private final OpeningTypeCatalogLoader catalogLoader = new OpeningTypeCatalogLoader();
	private final ApertureKernel kernel = ApertureKernel.builder()
		.withRegistry(openingTypes)
		.withProfiles(profileCatalog)
		.build();
	private final OpeningGenerationService generation = new OpeningGenerationService(kernel);
	private final PlacementService placement = new PlacementService(openingTypes, instances);
	private final FabricPlacementAdapter fabricPlacement = new FabricPlacementAdapter();
	private final OpeningInstanceRepository runtimeState = new OpeningInstanceRepository(instances);
	private final RuntimeObjectRegistry runtimeObjects = new RuntimeObjectRegistry(runtimeState);
	private final RuntimePipeline runtimePipeline = new RuntimePipeline(
		List.of(new OpeningRuntimeBehavior(openingTypes)), runtimeObjects
	);
	private final ArchitecturalRuntimeEnvironment runtimeEnvironment = new ArchitecturalRuntimeEnvironment(
		runtimeObjects,
		runtimePipeline,
		new RuntimeTransactionManager(),
		new RuntimeEventBus(),
		new RuntimeTickScheduler(),
		RuntimeWorldQuery.empty(),
		RuntimeReplicator.noop(),
		new RuntimeDiagnostics()
	);

	public void initialize() {
		registerBlocks();
		reloadKernelResources();
		loadMaterialCatalog();
		ApertureRuntime.init(new ApertureRuntime(
			openingTypes,
			profileCatalog,
			materialCatalog,
			materials,
			instances,
			generation,
			placement,
			runtimePipeline,
			runtimeEnvironment
		));
		LOGGER.info("Aperture runtime bootstrap complete - {} opening types", openingTypes.all().size());
	}

	private void registerBlocks() {
		ApertureBlocks.registerAll();
		ApertureBlockEntities.registerAll();
		LOGGER.info("Registered opening block + block entity");
	}

	public void reloadKernelResources() {
		List<OpeningTypeDefinition> definitions = List.of(
			catalogLoader.loadClasspathResource("aperture/opening_types/fixed_window.json"),
			catalogLoader.loadClasspathResource("aperture/opening_types/door.json"),
			catalogLoader.loadClasspathResource("aperture/opening_types/curtain_wall.json")
		);
		enforceReferenceTypeFreeze(definitions);
		var profiles = new ProfileCatalogLoader().loadClasspathDirectory("aperture/profiles");
		kernel.replaceResources(definitions, profiles);
		LOGGER.info(
			"Reloaded {} opening types (revision {}) and {} profiles (revision {})",
			openingTypes.all().size(), openingTypes.revision(),
			profileCatalog.all().size(), profileCatalog.revision()
		);
	}

	private void enforceReferenceTypeFreeze(Iterable<OpeningTypeDefinition> definitions) {
		for (OpeningTypeDefinition definition : definitions) {
			if (!BuiltinOpeningTypes.isReferenceType(definition.id())) {
				throw new IllegalStateException(
					"Family library freeze: unexpected opening type in catalog: " + definition.id()
				);
			}
		}
	}
	private void loadMaterialCatalog() {
		MaterialCatalogRegistry loaded = new MaterialCatalogLoader().loadClasspathCatalog();
		for (var definition : loaded.all().values()) {
			materialCatalog.register(definition);
		}
		for (var entry : loaded.slotDefaults().entrySet()) {
			materialCatalog.setSlotDefault(entry.getKey(), entry.getValue());
		}
		materials.setFallback(new CatalogMaterialResolver(materialCatalog));
		LOGGER.info("Loaded {} catalog materials", materialCatalog.all().size());
	}


	public OpeningTypeRegistry openingTypes() {
		return openingTypes;
	}

	public ProfileCatalogRegistry profileCatalog() {
		return profileCatalog;
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

	public FabricPlacementAdapter fabricPlacement() {
		return fabricPlacement;
	}
}
