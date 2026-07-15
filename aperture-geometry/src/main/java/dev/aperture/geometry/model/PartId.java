package dev.aperture.geometry.model;

import java.util.Objects;

/**
 * Stable identity for a renderable geometry part.
 * Derived from {@code GeometrySolid.componentPath()} during diffing.
 */
public record PartId(String componentPath) {
	public PartId {
		Objects.requireNonNull(componentPath, "componentPath");
		if (componentPath.isBlank()) {
			throw new IllegalArgumentException("componentPath must not be blank");
		}
	}

	public static PartId of(String componentPath) {
		return new PartId(componentPath);
	}
}
