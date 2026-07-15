package dev.aperture.geometry.generator;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.geometry.generator.pipeline.GenerationContext;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.profile.ProfileCatalogLoader;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;

/**
 * Test helpers for generation pipeline runs.
 */
public final class GenerationTestSupport {
	private static final ProfileCatalogRegistry PROFILES = new ProfileCatalogLoader().loadClasspathCatalog();

	private GenerationTestSupport() {
	}

	public static ProfileCatalogRegistry profiles() {
		return PROFILES;
	}

	public static GenerationContext context(OpeningTypeDefinition definition, ParameterSet overrides) {
		return new GenerationContext(
			definition,
			ParameterSet.mergeDefaults(definition.parameters(), overrides),
			PROFILES
		);
	}

	public static GeometryResult generateFixedWindow(ParameterSet overrides) {
		return new RectangularWindowGenerator().generate(context(BuiltinOpeningTypes.fixedWindow(), overrides));
	}

	public static GeometryResult generateCasementWindow(ParameterSet overrides) {
		return new RectangularWindowGenerator().generate(context(BuiltinOpeningTypes.casementWindow(), overrides));
	}

	public static GeometryResult generateFixedWindow() {
		return generateFixedWindow(ParameterSet.empty());
	}
}
