package dev.aperture.pipeline.stage;

import dev.aperture.geometry.model.CompositeGeometry;
import dev.aperture.geometry.pipeline.PipelineResult;
import dev.aperture.opening.component.ComponentPlan;
import dev.aperture.opening.pipeline.OpeningGenerationPipeline;
import dev.aperture.pipeline.PipelineStage;
import dev.aperture.pipeline.StageContext;
import dev.aperture.pipeline.StageResult;

import java.util.Objects;

/**
 * Geometry generation stage.
 * <p>
 * Executes the component plan to generate the opening geometry.
 * This involves instantiating components, executing their geometry
 * generators, and assembling the results into a composite geometry.
 * <p>
 * Input: {@link ComponentPlan} (component assembly blueprint)
 * Output: {@link CompositeGeometry} (generated opening geometry)
 */
public final class GeometryStage implements PipelineStage<ComponentPlan, CompositeGeometry> {

	private final OpeningGenerationPipeline geometryPipeline;

	/**
	 * Create geometry stage with standard pipeline.
	 */
	public GeometryStage() {
		this(OpeningGenerationPipeline.standard());
	}

	/**
	 * Create geometry stage with custom pipeline.
	 */
	public GeometryStage(OpeningGenerationPipeline geometryPipeline) {
		this.geometryPipeline = Objects.requireNonNull(geometryPipeline, "geometryPipeline cannot be null");
	}

	@Override
	public String name() {
		return "geometry";
	}

	@Override
	public StageResult<CompositeGeometry> execute(ComponentPlan input, StageContext ctx) {
		Objects.requireNonNull(input, "input cannot be null");

		ctx.debug("Executing component plan to generate geometry");

		try {
			// Execute geometry generation pipeline
			PipelineResult result = geometryPipeline.execute(input);

			if (!result.success()) {
				return new StageResult.Failure<>(
					"Geometry generation failed: " + result.error().orElse("Unknown error")
				);
			}

			CompositeGeometry geometry = result.geometry();

			ctx.debug("Generated geometry with " + geometry.solids().size() + " solids");

			return new StageResult.Success<>(geometry);

		} catch (Exception e) {
			return new StageResult.Failure<>(
				"Failed to generate geometry: " + e.getMessage(),
				e
			);
		}
	}
}
