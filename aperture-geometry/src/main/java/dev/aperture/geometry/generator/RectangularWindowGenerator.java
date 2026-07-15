package dev.aperture.geometry.generator;

import dev.aperture.geometry.generator.pipeline.GenerationContext;
import dev.aperture.geometry.pipeline.OpeningPipeline;
import dev.aperture.geometry.pipeline.PipelineResult;

/**
 * Reference generator: rectangular window through the standard opening pipeline.
 */
public final class RectangularWindowGenerator implements OpeningGenerator {
	public static final String ID = "aperture:rectangular_window_v1";

	private final OpeningPipeline pipeline = OpeningPipeline.standard();

	@Override
	public String id() {
		return ID;
	}

	@Override
	public PipelineResult generate(GenerationContext context) {
		return pipeline.execute(context);
	}

	public OpeningPipeline pipeline() {
		return pipeline;
	}
}
