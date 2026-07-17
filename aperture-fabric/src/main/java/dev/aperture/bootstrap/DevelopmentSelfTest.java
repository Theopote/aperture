package dev.aperture.bootstrap;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.service.OpeningGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/** Explicit development-only smoke test; never mutates the runtime instance store. */
public final class DevelopmentSelfTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("aperture/self-test");

	private DevelopmentSelfTest() { }

	public static boolean shouldRun(boolean developmentEnvironment, boolean explicitOption) {
		return developmentEnvironment || explicitOption;
	}

	public static void run(OpeningTypeRegistry openingTypes, OpeningGenerationService generation) {
		Objects.requireNonNull(openingTypes, "openingTypes");
		Objects.requireNonNull(generation, "generation");
		var definition = openingTypes.require(BuiltinOpeningTypes.FIXED_WINDOW_ID);
		OpeningInstance transientInstance = OpeningInstance.builder(definition.id())
			.parameters(ParameterSet.empty())
			.build();
		GeometryResult geometry = generation.generate(transientInstance).asSuccess().output().geometry();
		LOGGER.info(
			"Development self-test passed: reference window has {} solids, bounds {}x{}x{} mm",
			geometry.solids().size(),
			geometry.bounds().width(),
			geometry.bounds().height(),
			geometry.bounds().depth()
		);
	}
}
