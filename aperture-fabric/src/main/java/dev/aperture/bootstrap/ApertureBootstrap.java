package dev.aperture.bootstrap;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeCatalogLoader;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.InMemoryOpeningInstanceStore;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningInstanceStore;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.core.placement.PlacementService;
import dev.aperture.fabric.placement.FabricPlacementAdapter;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.profile.ProfileCatalogLoader;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.geometry.profile.ProfileDefinition;
import dev.aperture.opening.geometry.generator.RectangularWindowGenerator;
import dev.aperture.registry.ApertureBlockEntities;
import dev.aperture.registry.ApertureBlocks;
import dev.aperture.runtime.ApertureRuntime;
import dev.aperture.runtime.catalog.MaterialCatalogLoader;
import dev.aperture.runtime.catalog.MaterialCatalogRegistry;
import dev.aperture.runtime.material.CatalogMaterialResolver;
import dev.aperture.runtime.material.VanillaMaterialResolver;
import dev.aperture.runtime.registry.GeneratorRegistry;
import dev.aperture.runtime.registry.MaterialResolverRegistry;
import dev.aperture.runtime.service.OpeningGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wires runtime registries, data packs, generators, and services at mod startup.
 * Editor services are initialized separately on the client via {@link dev.aperture.editor.ApertureEditor}.
 */
public final class ApertureBootstrap {
	private static final Logger LOGGER = LoggerFactory.getLogger("aperture");

	private final OpeningTypeRegistry openingTypes = new OpeningTypeRegistry();
	private final GeneratorRegistry generators = new GeneratorRegistry();
	private final ProfileCatalogRegistry profileCatalog = new ProfileCatalogRegistry();
	private final MaterialCatalogRegistry materialCatalog = new MaterialCatalogRegistry();
	private final MaterialResolverRegistry materials = new MaterialResolverRegistry(VanillaMaterialResolver.INSTANCE);
	private final OpeningInstanceStore instances = new InMemoryOpeningInstanceStore();
	private final OpeningTypeCatalogLoader catalogLoader = new OpeningTypeCatalogLoader();
	private final OpeningGenerationService generation = new OpeningGenerationService(openingTypes, generators, profileCatalog);
	private final PlacementService placement = new PlacementService(openingTypes, instances);
	private final FabricPlacementAdapter fabricPlacement = new FabricPlacementAdapter();

	public void initialize() {
		registerBlocks();
		registerGenerators();
		loadOpeningTypes();
		loadProfileCatalog();
		loadMaterialCatalog();
		ApertureRuntime.init(new ApertureRuntime(
			openingTypes,
			generators,
			profileCatalog,
			materialCatalog,
			materials,
			instances,
			generation,
			placement
		));
		verifyReferencePipeline();
		LOGGER.info("Aperture runtime bootstrap complete - {} opening types, {} generators",
			openingTypes.all().size(), 1);
	}

	private void registerBlocks() {
		ApertureBlocks.registerAll();
		ApertureBlockEntities.registerAll();
		LOGGER.info("Registered opening block + block entity");
	}

	private void registerGenerators() {
		generators.register(new RectangularWindowGenerator());
	}

	private void loadOpeningTypes() {
		openingTypes.register(catalogLoader.loadClasspathResource("aperture/opening_types/fixed_window.json"));
		openingTypes.register(catalogLoader.loadClasspathResource("aperture/opening_types/door.json"));
		openingTypes.register(catalogLoader.loadClasspathResource("aperture/opening_types/curtain_wall.json"));
		enforceReferenceTypeFreeze();
		LOGGER.info("Loaded {} opening types from data pack", openingTypes.all().size());
	}

	private void enforceReferenceTypeFreeze() {
		for (OpeningTypeDefinition definition : openingTypes.all()) {
			if (!BuiltinOpeningTypes.isReferenceType(definition.id())) {
				throw new IllegalStateException(
					"Family library freeze: unexpected opening type in catalog: " + definition.id()
				);
			}
		}
	}

	private void loadProfileCatalog() {
		ProfileCatalogRegistry loaded = new ProfileCatalogLoader().loadClasspathCatalog();
		for (ProfileDefinition definition : loaded.all().values()) {
			profileCatalog.register(definition);
		}
		LOGGER.info("Loaded {} catalog profiles", profileCatalog.all().size());
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

	private void verifyReferencePipeline() {
		OpeningTypeDefinition definition = openingTypes.require(BuiltinOpeningTypes.FIXED_WINDOW_ID);
		OpeningInstance instance = OpeningInstance.builder(definition.id())
			.parameters(ParameterSet.empty())
			.build();

		instances.put(instance);
		GeometryResult geometry = generation.generate(instance);

		LOGGER.info("Reference window geometry: {} solids, bounds {}x{}x{} mm",
			geometry.solids().size(),
			geometry.bounds().width(),
			geometry.bounds().height(),
			geometry.bounds().depth());
	}

	public OpeningTypeRegistry openingTypes() {
		return openingTypes;
	}

	public GeneratorRegistry generators() {
		return generators;
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
