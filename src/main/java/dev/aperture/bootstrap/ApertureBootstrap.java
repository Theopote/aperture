package dev.aperture.bootstrap;

import dev.aperture.api.ApertureApi;
import dev.aperture.api.registry.GeneratorRegistry;
import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.validation.ParameterConstraintValidator;
import dev.aperture.core.validation.ValidationResult;
import dev.aperture.geometry.generators.RectangularWindowGenerator;
import dev.aperture.geometry.model.GeometryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wires core registries, builtin content, and generators at mod startup.
 */
public final class ApertureBootstrap {
	private static final Logger LOGGER = LoggerFactory.getLogger("aperture");

	private final OpeningTypeRegistry openingTypes = new OpeningTypeRegistry();
	private final GeneratorRegistry generators = new GeneratorRegistry();
	private final ParameterConstraintValidator parameterValidator = new ParameterConstraintValidator();

	public void initialize() {
		registerGenerators();
		registerBuiltinOpeningTypes();
		ApertureApi.init(new ApertureApi(openingTypes, generators));
		verifyReferencePipeline();
		LOGGER.info("Aperture bootstrap complete — {} opening types, {} generators",
			openingTypes.all().size(), 1);
	}

	private void registerGenerators() {
		generators.register(new RectangularWindowGenerator());
	}

	private void registerBuiltinOpeningTypes() {
		openingTypes.register(BuiltinOpeningTypes.fixedWindow());
	}

	private void verifyReferencePipeline() {
		OpeningTypeDefinition definition = openingTypes.require(BuiltinOpeningTypes.FIXED_WINDOW_ID);
		ParameterSet parameters = ParameterSet.mergeDefaults(definition.parameters(), ParameterSet.empty());
		OpeningInstance instance = OpeningInstance.builder(definition.id())
			.parameters(parameters)
			.build();

		ValidationResult validation = parameterValidator.validate(definition, instance);
		if (!validation.isValid()) {
			throw new IllegalStateException("Builtin fixed window failed validation: " + validation.issues());
		}

		GeometryResult geometry = generators.generate(definition, parameters);
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
}
