package dev.aperture.render.material;

import java.util.Objects;

/**
 * Resolved material with optional instance overrides.
 */
public record MaterialInstance(
	MaterialDefinition definition,
	int tintArgb
) {
	public MaterialInstance {
		Objects.requireNonNull(definition, "definition");
	}

	public static MaterialInstance of(MaterialDefinition definition) {
		return new MaterialInstance(definition, 0xFFFFFFFF);
	}
}
