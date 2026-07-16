package dev.aperture.opening.geometry.generator;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.opening.test.OpeningTestFixtures;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.PipelineResult;
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
			definition.resolveParameters(overrides),
			PROFILES
		);
	}

	public static PipelineResult generateFixedWindowPipeline(ParameterSet overrides) {
		return new RectangularWindowGenerator().generate(context(BuiltinOpeningTypes.fixedWindow(), overrides));
	}

	public static GeometryResult generateFixedWindow(ParameterSet overrides) {
		return generateFixedWindowPipeline(overrides).geometry();
	}

	public static GeometryResult generateFixedWindow() {
		return generateFixedWindow(ParameterSet.empty());
	}

	public static PipelineResult generateCasementWindowPipeline(ParameterSet overrides) {
		return new RectangularWindowGenerator().generate(context(OpeningTestFixtures.casementWindow(), overrides));
	}

	public static PipelineResult generateDoorPipeline(ParameterSet overrides) {
		return new RectangularWindowGenerator().generate(context(BuiltinOpeningTypes.door(), overrides));
	}

	public static PipelineResult generateCurtainWallPipeline(ParameterSet overrides) {
		return new RectangularWindowGenerator().generate(context(BuiltinOpeningTypes.curtainWall(), overrides));
	}

	public static GeometryResult generateCasementWindow(ParameterSet overrides) {
		return generateCasementWindowPipeline(overrides).geometry();
	}
}
