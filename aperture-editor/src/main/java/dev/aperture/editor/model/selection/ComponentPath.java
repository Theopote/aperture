package dev.aperture.editor.model.selection;

import java.util.Objects;

public record ComponentPath(String value) {
	public ComponentPath { Objects.requireNonNull(value); if (value.isBlank()) throw new IllegalArgumentException("component path is blank"); }
}
