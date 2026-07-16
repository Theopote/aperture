package dev.aperture.pipeline.stage;

import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.geometry.profile.ProfileCatalogLoader;
import dev.aperture.geometry.profile.ProfileCatalogRegistry;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.pipeline.OpeningGenerationPipeline;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/** Executes the current recipe-based opening geometry pipeline. */
public final class GeometryStage implements PipelineStage<ComponentStage.PlannedOpening, PipelineResult> {
	private final OpeningGenerationPipeline geometryPipeline;
	private final ProfileCatalogRegistry profiles;

	public GeometryStage() {
		this(OpeningGenerationPipeline.standard(), new ProfileCatalogLoader().loadClasspathCatalog());
	}

	public GeometryStage(OpeningGenerationPipeline geometryPipeline, ProfileCatalogRegistry profiles) {
		this.geometryPipeline = Objects.requireNonNull(geometryPipeline, "geometryPipeline cannot be null");
		this.profiles = Objects.requireNonNull(profiles, "profiles cannot be null");
	}

	@Override
	public String name() {
		return "geometry";
	}

	@Override
	public StageResult<PipelineResult> execute(ComponentStage.PlannedOpening input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");
		try {
			GenerationContext generationContext = new GenerationContext(
				input.typeDefinition(), input.parameters(), profiles
			);
			return new StageResult.Success<>(geometryPipeline.generate(generationContext));
		} catch (Exception exception) {
			return new StageResult.Failure<>("Failed to generate geometry: " + exception.getMessage(), exception);
		}
	}
}
