package dev.aperture.runtime.world;

import java.util.Map;
import java.util.Objects;

/** Typed, namespaced request for platform world state such as light, weather, or power. */
public record WorldQuery<T>(String id, Class<T> resultType, Map<String, Object> inputs) {
	public WorldQuery {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(resultType, "resultType");
		if (id.isBlank() || id.indexOf(':') <= 0 || id.endsWith(":")) {
			throw new IllegalArgumentException("World query id must be namespaced: " + id);
		}
		inputs = Map.copyOf(inputs);
	}
}
