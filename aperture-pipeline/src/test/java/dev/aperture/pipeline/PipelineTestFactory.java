package dev.aperture.pipeline;

import dev.aperture.core.catalog.BuiltinOpeningTypes;
import dev.aperture.core.catalog.OpeningTypeRegistry;
import dev.aperture.geometry.profile.ProfileCatalogLoader;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.opening.compile.OpeningGeometryCompiler;
import dev.aperture.opening.compile.OpeningMeshCompiler;
import dev.aperture.opening.component.ComponentPlanBuilder;
import dev.aperture.pipeline.adapter.OpeningPipelineAdapter;
import dev.aperture.pipeline.stage.ComponentStage;
import dev.aperture.pipeline.stage.GeometryStage;
import dev.aperture.pipeline.stage.MeshStage;

public final class PipelineTestFactory {
	private PipelineTestFactory() {
	}

	public static OpeningTypeRegistry registry() {
		OpeningTypeRegistry registry = new OpeningTypeRegistry();
		BuiltinOpeningTypes.referenceDefinitions().forEach(registry::register);
		return registry;
	}

	public static ProfileCatalogRegistry profiles() {
		return new ProfileCatalogLoader().loadClasspathCatalog();
	}

	public static OpeningPipelineAdapter adapter() {
		return OpeningPipelineAdapter.standard(registry(), profiles());
	}

	public static ComponentStage componentStage() {
		return new ComponentStage(new ComponentPlanBuilder());
	}

	public static GeometryStage geometryStage() {
		return new GeometryStage(new OpeningGeometryCompiler(), profiles());
	}

	public static MeshStage meshStage() {
		return new MeshStage(new OpeningMeshCompiler());
	}
}