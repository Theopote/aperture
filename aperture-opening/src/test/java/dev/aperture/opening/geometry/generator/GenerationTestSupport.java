package dev.aperture.opening.geometry.generator;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.geometry.profile.ProfileCatalogLoader;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.opening.compile.OpeningGeometryCompiler;
import dev.aperture.opening.compile.OpeningMeshCompiler;
import dev.aperture.opening.component.ComponentPlan;
import dev.aperture.opening.component.ComponentPlanBuilder;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.test.OpeningTestFixtures;
import dev.aperture.parameter.ParameterSet;

/** Test-only composition helpers for opening compiler tests. */
public final class GenerationTestSupport {
	private static final ProfileCatalogRegistry PROFILES = new ProfileCatalogLoader().loadClasspathCatalog();
	private static final ComponentPlanBuilder PLANNER = new ComponentPlanBuilder();
	private static final OpeningGeometryCompiler GEOMETRY = new OpeningGeometryCompiler();
	private static final OpeningMeshCompiler MESH = new OpeningMeshCompiler();

	private GenerationTestSupport() {
	}

	public static ProfileCatalogRegistry profiles() {
		return PROFILES;
	}

	public static GenerationContext context(OpeningTypeDefinition definition, ParameterSet overrides) {
		return new GenerationContext(definition, definition.resolveParameters(overrides), PROFILES);
	}

	public static ComponentPlan plan(GenerationContext context) {
		return PLANNER.build(context.definition().components());
	}

	public static PipelineResult compile(GenerationContext context) {
		ComponentPlan plan = plan(context);
		var geometry = GEOMETRY.compile(
			context.definition(), context.parameters(), plan, context.profiles()
		);
		var meshes = MESH.compile(geometry.result());
		return new PipelineResult(geometry.result(), meshes, geometry.recipe());
	}

	public static PipelineResult generateFixedWindowPipeline(ParameterSet overrides) {
		return compile(context(BuiltinOpeningTypes.fixedWindow(), overrides));
	}

	public static GeometryResult generateFixedWindow(ParameterSet overrides) {
		return generateFixedWindowPipeline(overrides).geometry();
	}

	public static GeometryResult generateFixedWindow() {
		return generateFixedWindow(ParameterSet.empty());
	}

	public static PipelineResult generateCasementWindowPipeline(ParameterSet overrides) {
		return compile(context(OpeningTestFixtures.casementWindow(), overrides));
	}

	public static PipelineResult generateDoorPipeline(ParameterSet overrides) {
		return compile(context(BuiltinOpeningTypes.door(), overrides));
	}

	public static PipelineResult generateCurtainWallPipeline(ParameterSet overrides) {
		return compile(context(BuiltinOpeningTypes.curtainWall(), overrides));
	}

	public static GeometryResult generateCasementWindow(ParameterSet overrides) {
		return generateCasementWindowPipeline(overrides).geometry();
	}
}