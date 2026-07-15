package dev.aperture.bootstrap;

import dev.aperture.api.ApertureApi;
import dev.aperture.api.registry.GeneratorRegistry;
import dev.aperture.api.service.OpeningGenerationService;
import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeCatalogLoader;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.InMemoryOpeningInstanceStore;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.instance.OpeningInstanceStore;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.generators.RectangularWindowGenerator;
import dev.aperture.geometry.model.GeometryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wires core registries, data packs, generators, and services at mod startup.
 */
public final class ApertureBootstrap {
	private static final Logger LOGGER = LoggerFactory.getLogger("aperture");

	private final OpeningTypeRegistry openingTypes = new OpeningTypeRegistry();
	private final GeneratorRegistry generators = new GeneratorRegistry();
	private final OpeningInstanceStore instances = new InMemoryOpeningInstanceStore();
	private final OpeningTypeCatalogLoader catalogLoader = new OpeningTypeCatalogLoader();
	private final OpeningGenerationService generation = new OpeningGenerationService(openingTypes, generators);

	public void initialize() {
		registerGenerators();
		loadOpeningTypes();
		ApertureApi.init(new ApertureApi(openingTypes, generators, instances, generation));
		verifyReferencePipeline();
		LOGGER.info("Aperture bootstrap complete — {} opening types, {} generators",
			openingTypes.all().size(), 1);
	}

	private void registerGenerators() {
		generators.register(new RectangularWindowGenerator());
	}

	private void loadOpeningTypes() {
		OpeningTypeDefinition fromData = catalogLoader.loadClasspathResource("aperture/opening_types/fixed_window.json");
		openingTypes.register(fromData);
		LOGGER.info("Loaded opening type from data pack: {}", fromData.id());
	}

	private void verifyReferencePipeline() {
		OpeningTypeDefinition definition = openingTypes.require(BuiltinOpeningTypes.FIXED_WINDOW_ID);
		ParameterSet parameters = ParameterSet.mergeDefaults(definition.parameters(), ParameterSet.empty());
		OpeningInstance instance = OpeningInstance.builder(definition.id())
			.parameters(parameters)
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

	public OpeningInstanceStore instances() {
		return instances;
	}

	public OpeningGenerationService generation() {
		return generation;
	}
}
