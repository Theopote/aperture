package dev.aperture.geometry.pipeline;

import dev.aperture.geometry.generator.pipeline.GenerationContext;

/**
 * Resolved opening parameters for the generator pipeline.
 */
public record OpeningParameters(
	double width,
	double height,
	int mullions,
	double openAngleDegrees,
	String panelHinge,
	boolean hasPanel
) {
	public static OpeningParameters from(GenerationContext context) {
		return new OpeningParameters(
			context.requireLength("width"),
			context.requireLength("height"),
			context.requireCount("mullions"),
			context.angleDegrees("open_angle", 0),
			context.componentString("panel", "hinge", "left"),
			context.hasComponent("panel")
		);
	}
}
