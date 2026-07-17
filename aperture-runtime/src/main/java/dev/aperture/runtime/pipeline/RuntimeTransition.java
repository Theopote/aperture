package dev.aperture.runtime.pipeline;

import dev.aperture.core.object.ArchitecturalObject;

import java.util.List;
import java.util.Objects;

/** State and world effects produced by one behavior evaluation. */
public record RuntimeTransition<T extends ArchitecturalObject>(T object, List<RuntimeEffect> worldEffects) {
	public RuntimeTransition {
		Objects.requireNonNull(object, "object");
		worldEffects = List.copyOf(worldEffects);
	}

	public static <T extends ArchitecturalObject> RuntimeTransition<T> unchanged(T object) {
		return new RuntimeTransition<>(object, List.of());
	}
}
