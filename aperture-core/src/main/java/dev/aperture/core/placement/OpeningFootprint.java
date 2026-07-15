package dev.aperture.core.placement;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.math.BoundingBox;
import dev.aperture.math.Vec3d;
import dev.aperture.core.instance.OpeningInstance;
import dev.aperture.core.parameter.ParameterSet;
import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;

import java.util.Optional;

/**
 * Derives axis-aligned opening bounds from instance parameters for placement checks.
 */
public final class OpeningFootprint {
	private static final double DEFAULT_DEPTH_MM = 100.0;

	private OpeningFootprint() {
	}

	public static Optional<BoundingBox> worldBounds(OpeningTypeDefinition definition, OpeningInstance instance) {
		ParameterSet parameters = definition.resolveParameters(instance.parameters());
		Optional<Double> width = length(parameters, "width");
		Optional<Double> height = length(parameters, "height");
		if (width.isEmpty() || height.isEmpty()) {
			return Optional.empty();
		}

		double depth = length(parameters, "frame_width").orElse(DEFAULT_DEPTH_MM);
		Vec3d origin = instance.transform().origin();
		return Optional.of(new BoundingBox(
			origin,
			new Vec3d(origin.x() + width.get(), origin.y() + height.get(), origin.z() + depth)
		));
	}

	private static Optional<Double> length(ParameterSet parameters, String name) {
		return parameters.get(name)
			.filter(value -> value.type() == ParameterType.LENGTH)
			.map(value -> ((ParameterValue.LengthValue) value).millimeters());
	}
}
