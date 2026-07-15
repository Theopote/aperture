package dev.aperture.geometry.generator;

import dev.aperture.geometry.generator.pipeline.FrameStage;
import dev.aperture.geometry.generator.pipeline.GenerationContext;
import dev.aperture.geometry.generator.pipeline.GenerationPipeline;
import dev.aperture.geometry.generator.pipeline.GlassStage;
import dev.aperture.geometry.generator.pipeline.PanelStage;
import dev.aperture.geometry.model.GeometryResult;

/**
 * Reference generator: rectangular window via frame, panel, and glass pipeline stages.
 */
public final class RectangularWindowGenerator implements OpeningGenerator {
	public static final String ID = "aperture:rectangular_window_v1";

	private final GenerationPipeline pipeline = GenerationPipeline.of(
		new FrameStage(),
		new PanelStage(),
		new GlassStage()
	);

	@Override
	public String id() {
		return ID;
	}

	@Override
	public GeometryResult generate(GenerationContext context) {
		return pipeline.execute(context);
	}
}
