package dev.aperture.opening.geometry.generator;

import dev.aperture.opening.component.ComponentPlan;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;
import dev.aperture.opening.pipeline.OpeningGenerationPipeline;
import dev.aperture.geometry.model.GeometryResult;
import dev.aperture.geometry.pipeline.PipelineResult;

/**
 * Generator entry point for the component-driven opening pipeline.
 * Window, door, and curtain wall all use this entry; they differ only by components.
 */
public final class RectangularWindowGenerator implements OpeningGenerator {
	public static final String ID = "aperture:rectangular_window_v1";

	private static final OpeningGenerationPipeline PIPELINE = OpeningGenerationPipeline.standard();

	@Override
	public String id() {
		return ID;
	}

	@Override
	public GeometryResult generateGeometry(GenerationContext context) {
		return PIPELINE.generateGeometry(context);
	}

	public PipelineResult generatePipeline(GenerationContext context) {
		return PIPELINE.generate(context);
	}

	public ComponentPlan planFor(GenerationContext context) {
		return PIPELINE.planFor(context);
	}

}
