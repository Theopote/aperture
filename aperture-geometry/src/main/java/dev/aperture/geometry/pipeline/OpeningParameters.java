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
	int cols,
	int rows,
	int panelCount,
	double glassRatio,
	double openAngleDegrees,
	String panelHinge,
	boolean hasPanel
) {
	public static OpeningParameters from(GenerationContext context) {
		int mullions = intParameter(context, "mullions", 0);
		int cols = intParameter(context, "cols", 1);
		int rows = intParameter(context, "rows", 1);
		int panelCount = intParameter(context, "panel_count", 1);
		double glassRatio = context.parameters().get("glass_ratio")
			.filter(value -> value.type() == ParameterType.NUMBER)
			.map(value -> ((ParameterValue.NumberValue) value).value())
			.orElse(1.0);
		return new OpeningParameters(
			context.requireLength("width"),
			context.requireLength("height"),
			mullions,
			Math.max(1, cols),
			Math.max(1, rows),
			Math.max(1, panelCount),
			clampRatio(glassRatio),
			context.angleDegrees("open_angle", 0),
			context.componentString("panel", "hinge", "left"),
			context.hasComponent(ComponentKind.PANEL)
		);
	}

	private static int intParameter(GenerationContext context, String name, int defaultValue) {
		return context.parameters().get(name)
			.filter(value -> value.type() == ParameterType.COUNT)
			.map(value -> ((ParameterValue.CountValue) value).value())
			.orElse(defaultValue);
	}

	private static double clampRatio(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}
}
