package dev.aperture.core.component;

import java.util.Objects;

/**
 * Stable identifier for one component instance within an opening assembly.
 */
public record ComponentRef(String id) {
	public ComponentRef {
		Objects.requireNonNull(id, "id");
		if (id.isBlank()) {
			throw new IllegalArgumentException("component id must not be blank");
		}
	}

	public static ComponentRef of(String id) {
		return new ComponentRef(id);
	}

	@Override
	public String toString() {
		return id;
	}
}
