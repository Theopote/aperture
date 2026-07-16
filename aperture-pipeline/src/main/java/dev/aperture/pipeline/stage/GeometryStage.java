package dev.aperture.pipeline.stage;

import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.opening.compile.OpeningGeometryCompiler;
import dev.aperture.opening.geometry.build.CompiledGeometry;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/** Compiles the existing definition, parameters, and component plan into geometry. */
public final class GeometryStage implements PipelineStage<ComponentStage.PlannedOpening, CompiledGeometry> {
	private final OpeningGeometryCompiler compiler;
	private final ProfileCatalogRegistry profiles;

	public GeometryStage(OpeningGeometryCompiler compiler, ProfileCatalogRegistry profiles) {
		this.compiler = Objects.requireNonNull(compiler, "compiler cannot be null");
		this.profiles = Objects.requireNonNull(profiles, "profiles cannot be null");
	}

	@Override
	public String name() {
		return "geometry";
	}

	@Override
	public StageResult<CompiledGeometry> execute(ComponentStage.PlannedOpening input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");
		try {
			return new StageResult.Success<>(compiler.compile(
				input.typeDefinition(), input.parameters(), input.plan(), profiles
			));
		} catch (Exception exception) {
			return new StageResult.Failure<>("Failed to compile geometry: " + exception.getMessage(), exception);
		}
	}
}