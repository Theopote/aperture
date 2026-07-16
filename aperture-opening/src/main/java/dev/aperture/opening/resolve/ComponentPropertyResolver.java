package dev.aperture.opening.resolve;

import dev.aperture.core.component.ComponentKind;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.opening.geometry.generator.pipeline.GenerationContext;

/**
 * Resolves component-owned properties from bound instances and parameter overrides.
 */
public final class ComponentPropertyResolver {
	private ComponentPropertyResolver() {
	}

	public static String panelHinge(GenerationContext context, String defaultHinge) {
		return context.parameters().get("hinge_side")
			.filter(value -> value.type() == ParameterType.ENUM)
			.map(value -> ((ParameterValue.EnumValue) value).value())
			.orElse(defaultHinge);
	}

	public static boolean hasKind(GenerationContext context, ComponentKind kind) {
		return context.components().has(kind);
	}
}
