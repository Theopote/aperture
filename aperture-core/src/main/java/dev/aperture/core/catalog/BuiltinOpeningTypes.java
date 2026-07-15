package dev.aperture.core.catalog;

import dev.aperture.core.definition.OpeningTypeDefinition;
import dev.aperture.core.opening.GeneratorId;
import dev.aperture.core.opening.OpeningCategory;
import dev.aperture.core.opening.OpeningId;
import dev.aperture.core.parameter.ParameterDefinition;
import dev.aperture.core.parameter.ParameterType;
import dev.aperture.core.parameter.ParameterValue;

import java.util.Map;

/**
 * Built-in reference opening types for Phase 0 development and testing.
 */
public final class BuiltinOpeningTypes {
	public static final OpeningId FIXED_WINDOW_ID = OpeningId.aperture("fixed_window");
	public static final GeneratorId RECTANGULAR_WINDOW_GENERATOR = GeneratorId.parse("aperture:rectangular_window_v1");

	public static OpeningTypeDefinition fixedWindow() {
		return OpeningTypeDefinition.builder(FIXED_WINDOW_ID, OpeningCategory.WINDOW, RECTANGULAR_WINDOW_GENERATOR)
			.parameter("width", ParameterDefinition.builder(ParameterType.LENGTH)
				.defaultValue(ParameterValue.length(1200))
				.min(300)
				.max(6000)
				.build())
			.parameter("height", ParameterDefinition.builder(ParameterType.LENGTH)
				.defaultValue(ParameterValue.length(1500))
				.min(300)
				.max(4000)
				.build())
			.parameter("mullions", ParameterDefinition.builder(ParameterType.COUNT)
				.defaultValue(ParameterValue.count(0))
				.min(0)
				.max(10)
				.build())
			.parameter("frame_width", ParameterDefinition.builder(ParameterType.LENGTH)
				.defaultValue(ParameterValue.length(50))
				.min(20)
				.max(150)
				.build())
			.materialSlot("frame")
			.materialSlot("glazing")
			.component("frame", Map.of("profile", "aperture:frame_l_50x80"))
			.component("glazing", Map.of("system", "aperture:single_glazed"))
			.build();
	}

	private BuiltinOpeningTypes() {
	}
}
