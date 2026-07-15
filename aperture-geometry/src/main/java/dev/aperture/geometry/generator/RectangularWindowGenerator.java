package dev.aperture.geometry.generator;

import dev.aperture.geometry.generator.pipeline.GenerationContext;
import dev.aperture.geometry.pipeline.ComponentPipelinePlanner;
import dev.aperture.geometry.pipeline.OpeningPipeline;
import dev.aperture.geometry.pipeline.PipelineResult;

/**
 * Reference generator: routes any component assembly through the component-driven pipeline.
 */
public final class RectangularWindowGenerator implements OpeningGenerator {
	public static final String ID = "aperture:rectangular_window_v1";

	@Override
	public String id() {
		return ID;
	}

	@Override
	public PipelineResult generate(GenerationContext context) {
		return pipelineFor(context).execute(context);
	}

	public OpeningPipeline pipelineFor(GenerationContext context) {
		return ComponentPipelinePlanner.pipelineFor(context.definition().components());
	}
}
