package dev.aperture.core.parametric;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.instance.OpeningState;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.parameter.ParameterValue;

/**
 * Maps runtime {@link OpeningState} into effective parametric values for geometry generation.
 */
final class OpeningStateParameters {
	private OpeningStateParameters() {
	}

	static ParameterSet apply(OpeningTypeDefinition definition, ParameterSet resolved, OpeningState state) {
		if (state.openRatio() <= 0.0 || !definition.parametricSchema().get("open_angle").isPresent()) {
			return resolved;
		}
		double maxDegrees = maxOpenAngleDegrees(definition, resolved);
		if (maxDegrees <= 0.0) {
			return resolved;
		}
		double effective = maxDegrees * state.openRatio();
		return withOpenAngle(resolved, effective);
	}

	private static double maxOpenAngleDegrees(OpeningTypeDefinition definition, ParameterSet resolved) {
		Parameter parameter = definition.parametricSchema().require("open_angle");
		double configured = resolved.angleOrDefault("open_angle", 0.0);
		if (configured > 0.0) {
			return configured;
		}
		return switch (parameter) {
			case RangeParameter range -> range.max();
			case NumberParameter number -> number.max().orElse(configured);
			default -> configured;
		};
	}

	private static ParameterSet withOpenAngle(ParameterSet resolved, double degrees) {
		ParameterSet.Builder builder = ParameterSet.builder();
		for (var entry : resolved.asMap().entrySet()) {
			if ("open_angle".equals(entry.getKey())) {
				builder.put(entry.getKey(), ParameterValue.angle(degrees));
			} else {
				builder.put(entry.getKey(), entry.getValue());
			}
		}
		if (!resolved.asMap().containsKey("open_angle")) {
			builder.put("open_angle", ParameterValue.angle(degrees));
		}
		return builder.build();
	}
}
