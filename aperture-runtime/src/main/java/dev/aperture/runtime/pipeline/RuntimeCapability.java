package dev.aperture.runtime.pipeline;

import java.util.Objects;

/** A namespaced operation an architectural object currently supports. */
public record RuntimeCapability(String id) {
	public RuntimeCapability {
		Objects.requireNonNull(id, "id");
		if (id.isBlank() || id.indexOf(':') <= 0 || id.endsWith(":")) {
			throw new IllegalArgumentException("Capability id must be namespaced: " + id);
		}
	}
}
