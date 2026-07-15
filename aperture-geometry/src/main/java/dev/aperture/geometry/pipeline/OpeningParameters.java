package dev.aperture.geometry.pipeline;

import dev.aperture.core.component.ComponentKind;
import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;
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
		int mullions = context.parameters().get("mullions")
			.filter(value -> value.type() == ParameterType.COUNT)
			.map(value -> ((ParameterValue.CountValue) value).value())
			.orElse(0);
		return new OpeningParameters(
			context.requireLength("width"),
			context.requireLength("height"),
			mullions,
			context.angleDegrees("open_angle", 0),
			context.componentString("panel", "hinge", "left"),
			context.hasComponent(ComponentKind.PANEL)
		);
	}
}
